/// Find bundled backend.jar in the application's resource directory
fn find_bundled_backend_jar() -> Option<std::path::PathBuf> {
    // In a packaged app, resources are in:
    // - Windows: app.exe directory or resources/ subdirectory
    // - macOS: OpenTron.app/Contents/Resources/
    // - Linux: app directory or resources/ subdirectory
    
    if let Ok(exe) = std::env::current_exe() {
        if let Some(exe_dir) = exe.parent() {
            // Check direct sibling
            let jar = exe_dir.join("backend.jar");
            if jar.exists() {
                return Some(jar);
            }
            
            // Check resources/ subdirectory
            let jar = exe_dir.join("resources").join("backend.jar");
            if jar.exists() {
                return Some(jar);
            }
            
            // macOS bundle: app.app/Contents/Resources/
            let jar = exe_dir.join("Resources").join("backend.jar");
            if jar.exists() {
                return Some(jar);
            }
        }
    }
    None
}

fn find_java_backend_or_bundled() -> Option<(String, std::path::PathBuf)> {
    let java_bin = resolve_bin("java");
    if !std::path::Path::new(&java_bin).exists() {
        return None;
    }

    // First, try to find bundled JAR in the application resources
    if let Some(jar) = find_bundled_backend_jar() {
        return Some((java_bin, jar));
    }

    // Fall back to looking in project directories (for development)
    find_java_backend()
}
