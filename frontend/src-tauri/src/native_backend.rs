// Native backend resolver module for GraalVM native-image binaries

use std::path::PathBuf;

/// Resolve a GraalVM native binary, preferring bundled sidecar over system paths.
/// Returns (binary_path, is_native). None if not found.
pub fn find_native_backend() -> Option<(String, bool)> {
    let app_root = std::env::current_exe().ok()?;
    let app_dir = app_root.parent()?;

    // Platform-specific native binary names
    let native_names = if cfg!(target_os = "windows") {
        vec!["opentron-backend-windows-x86_64.exe", "opentron-backend.exe"]
    } else if cfg!(target_os = "macos") {
        if cfg!(target_arch = "aarch64") {
            vec!["opentron-backend-macos-aarch64", "opentron-backend"]
        } else {
            vec!["opentron-backend-macos-x86_64", "opentron-backend"]
        }
    } else if cfg!(target_os = "linux") {
        if cfg!(target_arch = "aarch64") {
            vec!["opentron-backend-linux-aarch64", "opentron-backend"]
        } else {
            vec!["opentron-backend-linux-x86_64", "opentron-backend"]
        }
    } else {
        vec!["opentron-backend"]
    };

    // 1. Check app bundle / sidecar location
    for name in &native_names {
        let bundled = app_dir.join(name);
        if bundled.exists() {
            return Some((bundled.to_string_lossy().to_string(), true));
        }
    }

    None
}

/// Returns the platform-specific native binary name for the current system.
pub fn platform_native_name() -> &'static str {
    if cfg!(target_os = "windows") {
        "opentron-backend-windows-x86_64.exe"
    } else if cfg!(target_os = "macos") {
        if cfg!(target_arch = "aarch64") {
            "opentron-backend-macos-aarch64"
        } else {
            "opentron-backend-macos-x86_64"
        }
    } else if cfg!(target_os = "linux") {
        if cfg!(target_arch = "aarch64") {
            "opentron-backend-linux-aarch64"
        } else {
            "opentron-backend-linux-x86_64"
        }
    } else {
        "opentron-backend"
    }
}
