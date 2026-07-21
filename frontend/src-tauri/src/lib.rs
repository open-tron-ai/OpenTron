use std::sync::Arc;
use std::time::Duration;
use tauri::menu::{MenuBuilder, MenuItemBuilder};
use tauri::tray::TrayIconBuilder;
use tauri::Manager;
use tokio::sync::Mutex;

const TRON_PORT: u16 = 8000;
const BACKEND_STARTUP_TIMEOUT: Duration = Duration::from_secs(30);

// ============================================================================
// Backend Manager - starts and manages the Java backend process
// ============================================================================

struct ChildHandle {
    child: tokio::process::Child,
}

impl ChildHandle {
    async fn kill(&mut self) {
        let _ = self.child.kill().await;
    }
}

struct BackendManager {
    backend: Option<ChildHandle>,
    stderr_tail: Arc<Mutex<Vec<u8>>>,
}

impl Default for BackendManager {
    fn default() -> Self {
        Self {
            backend: None,
            stderr_tail: Arc::new(Mutex::new(Vec::new())),
        }
    }
}

impl BackendManager {
    async fn stop_all(&mut self) {
        if let Some(ref mut h) = self.backend {
            h.kill().await;
        }
        self.backend = None;
    }
}

type SharedBackend = Arc<Mutex<BackendManager>>;

// ============================================================================
// Helpers
// ============================================================================

fn home_dir() -> String {
    std::env::var("HOME")
        .or_else(|_| std::env::var("USERPROFILE"))
        .unwrap_or_default()
}

fn backend_jar_path() -> std::path::PathBuf {
    std::path::PathBuf::from(home_dir())
        .join(".OpenTron")
        .join("sidecar")
        .join("backend.jar")
}

fn jre_java_path() -> std::path::PathBuf {
    let home = home_dir();
    #[cfg(target_os = "windows")]
    {
        std::path::PathBuf::from(home)
            .join(".OpenTron")
            .join("sidecar")
            .join("jre")
            .join("bin")
            .join("java.exe")
    }
    #[cfg(not(target_os = "windows"))]
    {
        std::path::PathBuf::from(home)
            .join(".OpenTron")
            .join("sidecar")
            .join("jre")
            .join("bin")
            .join("java")
    }
}

async fn wait_for_url(url: &str, timeout: Duration) -> bool {
    let client = reqwest::Client::builder()
        .timeout(Duration::from_secs(2))
        .build()
        .unwrap();
    let deadline = tokio::time::Instant::now() + timeout;
    while tokio::time::Instant::now() < deadline {
        if let Ok(resp) = client.get(url).send().await {
            if resp.status().is_success() {
                return true;
            }
        }
        tokio::time::sleep(Duration::from_millis(500)).await;
    }
    false
}

fn spawn_stderr_drainer(
    stderr: tokio::process::ChildStderr,
    tail: Arc<Mutex<Vec<u8>>>,
) {
    use tokio::io::AsyncReadExt;
    tokio::spawn(async move {
        let mut reader = stderr;
        let mut buf = vec![0u8; 4096];
        loop {
            match reader.read(&mut buf).await {
                Ok(0) => break,
                Err(_) => break,
                Ok(n) => {
                    let mut t = tail.lock().await;
                    t.extend_from_slice(&buf[..n]);
                    if t.len() > 16 * 1024 {
                        let drop_n = t.len() - 16 * 1024;
                        t.drain(..drop_n);
                    }
                }
            }
        }
    });
}

// ============================================================================
// Boot Sequence
// ============================================================================

async fn boot_backend(backend: SharedBackend) -> Result<(), String> {
    let jar_path = backend_jar_path();
    let java_path = jre_java_path();

    if !jar_path.exists() {
        return Err(format!("Backend JAR not found: {}", jar_path.display()));
    }

    if !java_path.exists() {
        return Err(format!(
            "Java runtime not found: {}. Please reinstall the application.",
            java_path.display()
        ));
    }

    let mut cmd = tokio::process::Command::new(&java_path);
    cmd.arg("-Dspring.profiles.active=embedded")
        .arg("-jar")
        .arg(&jar_path)
        .arg(format!("--server.port={}", TRON_PORT))
        .stdout(std::process::Stdio::null())
        .stderr(std::process::Stdio::piped());

    let mut child = cmd
        .spawn()
        .map_err(|e| format!("Failed to start Java backend: {}", e))?;

    let stderr = child.stderr.take();
    let mut mgr = backend.lock().await;
    let tail = mgr.stderr_tail.clone();
    mgr.backend = Some(ChildHandle { child });
    drop(mgr);

    if let Some(stderr) = stderr {
        spawn_stderr_drainer(stderr, tail);
    }

    let health_url = format!("http://127.0.0.1:{}/actuator/health", TRON_PORT);
    if !wait_for_url(&health_url, BACKEND_STARTUP_TIMEOUT).await {
        let mut mgr = backend.lock().await;
        mgr.stop_all().await;
        return Err(
            "Backend failed to start. Check that the bundled JRE and JAR are present."
                .to_string(),
        );
    }

    Ok(())
}

// ============================================================================
// Tauri Commands
// ============================================================================

#[tauri::command]
fn get_api_base() -> String {
    format!("http://127.0.0.1:{}", TRON_PORT)
}

#[tauri::command]
async fn start_backend(backend: tauri::State<'_, SharedBackend>) -> Result<(), String> {
    boot_backend(backend.inner().clone()).await
}

#[tauri::command]
async fn stop_backend(backend: tauri::State<'_, SharedBackend>) -> Result<(), String> {
    backend.lock().await.stop_all().await;
    Ok(())
}

#[tauri::command]
async fn check_health() -> Result<bool, String> {
    let url = format!("http://127.0.0.1:{}/actuator/health", TRON_PORT);
    match reqwest::get(&url).await {
        Ok(resp) => Ok(resp.status().is_success()),
        Err(_) => Ok(false),
    }
}

#[tauri::command]
async fn fetch_models(api_url: String) -> Result<Vec<serde_json::Value>, String> {
    let url = if api_url.is_empty() {
        format!("http://127.0.0.1:{}/v1/models", TRON_PORT)
    } else {
        format!("{}/v1/models", api_url)
    };

    match reqwest::get(&url).await {
        Ok(resp) => match resp.json::<serde_json::Value>().await {
            Ok(data) => {
                if let Some(models) = data.get("data").and_then(|d| d.as_array()) {
                    Ok(models.clone())
                } else {
                    Ok(vec![])
                }
            }
            Err(_) => Err("Failed to parse models response".to_string()),
        },
        Err(e) => Err(format!("Failed to fetch models: {}", e)),
    }
}

#[tauri::command]
async fn fetch_server_info(api_url: String) -> Result<serde_json::Value, String> {
    let url = if api_url.is_empty() {
        format!("http://127.0.0.1:{}/v1/info", TRON_PORT)
    } else {
        format!("{}/v1/info", api_url)
    };

    match reqwest::get(&url).await {
        Ok(resp) => resp
            .json::<serde_json::Value>()
            .await
            .map_err(|e| format!("Failed to parse response: {}", e)),
        Err(e) => Err(format!("Failed to fetch server info: {}", e)),
    }
}

#[tauri::command]
async fn fetch_savings(api_url: String) -> Result<serde_json::Value, String> {
    let url = if api_url.is_empty() {
        format!("http://127.0.0.1:{}/v1/savings", TRON_PORT)
    } else {
        format!("{}/v1/savings", api_url)
    };

    match reqwest::get(&url).await {
        Ok(resp) => resp
            .json::<serde_json::Value>()
            .await
            .map_err(|e| format!("Failed to parse response: {}", e)),
        Err(e) => Err(format!("Failed to fetch savings: {}", e)),
    }
}

#[tauri::command]
async fn fetch_agents(api_url: String) -> Result<serde_json::Value, String> {
    let url = if api_url.is_empty() {
        format!("http://127.0.0.1:{}/v1/agents", TRON_PORT)
    } else {
        format!("{}/v1/agents", api_url)
    };

    match reqwest::get(&url).await {
        Ok(resp) => resp
            .json::<serde_json::Value>()
            .await
            .map_err(|e| format!("Failed to parse response: {}", e)),
        Err(e) => Err(format!("Failed to fetch agents: {}", e)),
    }
}

// ============================================================================
// App Entry Point
// ============================================================================

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    let backend: SharedBackend = Arc::new(Mutex::new(BackendManager::default()));
    let boot_backend_ref = backend.clone();

    tauri::Builder::default()
        .manage(backend.clone())
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_process::init())
        .plugin(tauri_plugin_notification::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_global_shortcut::Builder::new().build())
        .plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| {
            if let Some(window) = app.get_webview_window("main") {
                let _ = window.set_focus();
            }
        }))
        .setup(move |app| {
            // System tray
            let show = MenuItemBuilder::with_id("show", "Show / Hide").build(app)?;
            let health = MenuItemBuilder::with_id("health", "Health: starting...")
                .enabled(false)
                .build(app)?;
            let quit = MenuItemBuilder::with_id("quit", "Quit OpenTron").build(app)?;

            let menu = MenuBuilder::new(app)
                .item(&show)
                .separator()
                .item(&health)
                .separator()
                .item(&quit)
                .build()?;

            let _tray = TrayIconBuilder::with_id("main")
                .icon(app.default_window_icon().unwrap().clone())
                .tooltip("OpenTron")
                .menu(&menu)
                .on_menu_event(move |app, event| match event.id().as_ref() {
                    "show" => {
                        if let Some(window) = app.get_webview_window("main") {
                            if window.is_visible().unwrap_or(false) {
                                let _ = window.hide();
                            } else {
                                let _ = window.show();
                                let _ = window.set_focus();
                            }
                        }
                    }
                    "quit" => {
                        app.exit(0);
                    }
                    _ => {}
                })
                .build(app)?;

            // Auto-start Java backend on launch
            let b = boot_backend_ref.clone();
            tauri::async_runtime::spawn(async move {
                if let Err(e) = boot_backend(b).await {
                    eprintln!("Failed to start backend: {}", e);
                }
            });

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            get_api_base,
            start_backend,
            stop_backend,
            check_health,
            fetch_models,
            fetch_server_info,
            fetch_savings,
            fetch_agents,
        ])
        .build(tauri::generate_context!())
        .expect("error while building OpenTron Desktop")
        .run(move |_app, event| {
            if let tauri::RunEvent::ExitRequested { .. } = event {
                let b = backend.clone();
                tauri::async_runtime::spawn(async move {
                    b.lock().await.stop_all().await;
                });
            }
        });
}
