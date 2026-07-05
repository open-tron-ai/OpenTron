package org.opentron.backend.controllers;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.opentron.backend.storage.repositories.GeneratedProjectRepository;
import org.opentron.backend.storage.entities.GeneratedProject;
import org.opentron.backend.storage.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Projects API
 * Manage generated projects with persistent storage and trace logging
 */
@RestController
@RequestMapping("/v1/projects")
public class ProjectsController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectsController.class);

    @Autowired
    private GeneratedProjectRepository projectRepository;
    
    @Autowired
    private StorageService storageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * POST /v1/projects/create
     * Create and save a new project with trace logging
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createProject(@RequestBody org.opentron.backend.dto.CreateProjectRequest request) {
        long startTime = System.currentTimeMillis();
        try {
            String projectName = request.getName() == null ? "Untitled" : request.getName();
            String projectType = request.getType() == null ? "React" : request.getType();
            String description = request.getDescription() == null ? "" : request.getDescription();
            
            // Generate unique project ID
            String projectId = "proj_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            
            logger.info("Creating project: {} (ID: {})", projectName, projectId);
            
            // Generate sample files based on type
            Map<String, String> files = generateSampleFiles(projectType);
            long totalSize = files.values().stream().mapToLong(String::length).sum();
            
            // Create project entity
            GeneratedProject project = new GeneratedProject(
                projectId, projectName, projectType, 
                getFramework(projectType), getLanguage(projectType),
                files.size(), totalSize
            );
            project.setDescription(description);
            project.setFileList(String.join(", ", files.keySet()));
            project.setFilesJson(objectMapper.writeValueAsString(files));
            
            // Save to database
            GeneratedProject saved = projectRepository.save(project);
            
            logger.info("Project saved with ID: {}", saved.getProjectId());
            
            // Log trace
            long duration = System.currentTimeMillis() - startTime;
            try {
                String traceOutput = "Project created: " + projectName + " (" + files.size() + " files, " + totalSize + " bytes)";
                storageService.saveTrace("project-generator", "Create project: " + projectName, traceOutput, (int)duration);
                logger.debug("Trace logged for project creation");
            } catch (Exception e) {
                logger.warn("Failed to log trace", e);
            }
            
            // Return response with files
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("project_id", projectId);
            response.put("name", projectName);
            response.put("type", projectType);
            response.put("framework", getFramework(projectType));
            response.put("language", getLanguage(projectType));
            response.put("file_count", files.size());
            response.put("size_bytes", totalSize);
            response.put("size_mb", String.format("%.2f", totalSize / 1024.0 / 1024.0));
            response.put("created_at", saved.getCreatedAt());
            response.put("duration_ms", duration);
            response.put("files", files);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error creating project", e);
            
            // Log error trace
            try {
                storageService.saveTrace("project-generator", "Create project failed", "Error: " + e.getMessage(), (int)duration);
            } catch (Exception logErr) {
                logger.warn("Failed to log error trace", logErr);
            }
            
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", "error");
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /v1/projects
     * List all created projects with trace logging
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listProjects(
        @RequestParam(defaultValue = "50") int limit
    ) {
        long startTime = System.currentTimeMillis();
        try {
            List<GeneratedProject> projects = projectRepository.findAllByOrderByCreatedAtDesc();
            
            List<Map<String, Object>> projectsList = new ArrayList<>();
            for (GeneratedProject p : projects) {
                if (projectsList.size() >= limit) break;
                
                Map<String, Object> proj = new LinkedHashMap<>();
                proj.put("project_id", p.getProjectId());
                proj.put("name", p.getProjectName());
                proj.put("type", p.getProjectType());
                proj.put("framework", p.getFramework());
                proj.put("language", p.getLanguage());
                proj.put("file_count", p.getFileCount());
                proj.put("size_bytes", p.getSizeBytes());
                proj.put("size_mb", String.format("%.2f", p.getSizeBytes() / 1024.0 / 1024.0));
                proj.put("created_at", p.getCreatedAt().toString());
                projectsList.add(proj);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Log trace for list operation
            try {
                storageService.saveTrace("project-generator", "List projects (limit=" + limit + ")", "Listed " + projectsList.size() + " projects", (int)duration);
            } catch (Exception e) {
                logger.warn("Failed to log trace", e);
            }
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("projects", projectsList);
            response.put("count", projectsList.size());
            response.put("total_count", projectRepository.count());
            response.put("duration_ms", duration);
            
            logger.info("Listed {} projects", projectsList.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error listing projects", e);
            
            try {
                storageService.saveTrace("project-generator", "List projects failed", "Error: " + e.getMessage(), (int)duration);
            } catch (Exception logErr) {
                logger.warn("Failed to log error trace", logErr);
            }
            
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", "error");
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /v1/projects/{projectId}
     * Get a specific project with trace logging
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<Map<String, Object>> getProject(@PathVariable String projectId) {
        long startTime = System.currentTimeMillis();
        try {
            Optional<GeneratedProject> optional = projectRepository.findByProjectId(projectId);
            
            if (optional.isEmpty()) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("status", "error");
                error.put("error", "Project not found");
                
                long duration = System.currentTimeMillis() - startTime;
                try {
                    storageService.saveTrace("project-generator", "Get project: " + projectId, "Project not found", (int)duration);
                } catch (Exception e) {
                        logger.warn("Failed to log trace", e);
                }
                
                return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
            }
            
            GeneratedProject p = optional.get();
            Map<String, Object> files = objectMapper.readValue(p.getFilesJson(), Map.class);
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Log trace for get operation
            try {
                storageService.saveTrace("project-generator", "Get project: " + projectId, "Retrieved " + p.getFileCount() + " files", (int)duration);
            } catch (Exception e) {
                logger.warn("Failed to log trace", e);
            }
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("project_id", p.getProjectId());
            response.put("name", p.getProjectName());
            response.put("type", p.getProjectType());
            response.put("framework", p.getFramework());
            response.put("language", p.getLanguage());
            response.put("description", p.getDescription());
            response.put("file_count", p.getFileCount());
            response.put("size_bytes", p.getSizeBytes());
            response.put("size_mb", String.format("%.2f", p.getSizeBytes() / 1024.0 / 1024.0));
            response.put("created_at", p.getCreatedAt());
            response.put("duration_ms", duration);
            response.put("files", files);
            
            logger.info("Retrieved project: {}", projectId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error getting project {}", projectId, e);
            
            // Log trace for error
            try {
                storageService.saveTrace("project-generator", "Get project: " + projectId, "Error: " + e.getMessage(), (int)duration);
            } catch (Exception logErr) {
                logger.warn("Failed to log error trace", logErr);
            }
            
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", "error");
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Generate sample files based on project type
     */
    private Map<String, String> generateSampleFiles(String projectType) {
        Map<String, String> files = new LinkedHashMap<>();
        
        if (projectType.toLowerCase().contains("react")) {
            files.put("package.json", "{\"name\":\"auth-app\",\"version\":\"1.0.0\",\"type\":\"module\",\"scripts\":{\"dev\":\"vite\",\"build\":\"vite build\"},\"dependencies\":{\"react\":\"^18.3.0\",\"react-dom\":\"^18.3.0\",\"react-router-dom\":\"^6.22.0\",\"axios\":\"^1.6.0\"}}");
            files.put("src/main.tsx", "import React from 'react'\nimport ReactDOM from 'react-dom/client'\nimport App from './App'\nimport './index.css'\n\nReactDOM.createRoot(document.getElementById('root')!).render(\n  <React.StrictMode>\n    <App />\n  </React.StrictMode>,\n)");
            files.put("src/App.tsx", "import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'\nimport Login from './pages/Login'\nimport Dashboard from './pages/Dashboard'\nimport './App.css'\n\nfunction App() {\n  return (\n    <Router>\n      <Routes>\n        <Route path=\"/login\" element={<Login />} />\n        <Route path=\"/dashboard\" element={<Dashboard />} />\n        <Route path=\"/\" element={<Login />} />\n      </Routes>\n    </Router>\n  )\n}\n\nexport default App");
            files.put("src/pages/Login.tsx", "import { useState } from 'react'\nimport { useNavigate } from 'react-router-dom'\n\nexport default function Login() {\n  const [email, setEmail] = useState('')\n  const [password, setPassword] = useState('')\n  const navigate = useNavigate()\n\n  const handleSubmit = (e: React.FormEvent) => {\n    e.preventDefault()\n    localStorage.setItem('token', 'demo-token')\n    navigate('/dashboard')\n  }\n\n  return (\n    <div className=\"auth-container\">\n      <form onSubmit={handleSubmit}>\n        <h1>Login</h1>\n        <input type=\"email\" placeholder=\"Email\" value={email} onChange={(e) => setEmail(e.target.value)} required />\n        <input type=\"password\" placeholder=\"Password\" value={password} onChange={(e) => setPassword(e.target.value)} required />\n        <button type=\"submit\">Login</button>\n      </form>\n    </div>\n  )\n}");
            files.put("src/pages/Dashboard.tsx", "export default function Dashboard() {\n  return (\n    <div className=\"dashboard\">\n      <h1>Dashboard</h1>\n      <p>Welcome to your dashboard</p>\n    </div>\n  )\n}");
            files.put("src/App.css", "body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; }\n.auth-container { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1); width: 100%; max-width: 400px; }\nform { display: flex; flex-direction: column; gap: 1rem; }\ninput { padding: 0.75rem; border: 1px solid #ddd; border-radius: 4px; font-size: 1rem; }\nbutton { padding: 0.75rem; background: #667eea; color: white; border: none; border-radius: 4px; font-size: 1rem; cursor: pointer; }\nbutton:hover { background: #764ba2; }\n.dashboard { padding: 2rem; }\n");
            files.put("README.md", "# React Authentication App\n\nA production-ready user authentication system.\n\n## Features\n- User login with JWT\n- Protected dashboard\n- Responsive design\n- State management\n\n## Getting Started\n\n```bash\nnpm install\nnpm run dev\n```\n\n## License\n\nMIT");
        } else if (projectType.toLowerCase().contains("spring")) {
            files.put("pom.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>com.example</groupId>\n  <artifactId>user-api</artifactId>\n  <version>1.0.0</version>\n  <parent>\n    <groupId>org.springframework.boot</groupId>\n    <artifactId>spring-boot-starter-parent</artifactId>\n    <version>3.1.6</version>\n  </parent>\n  <dependencies>\n    <dependency>\n      <groupId>org.springframework.boot</groupId>\n      <artifactId>spring-boot-starter-web</artifactId>\n    </dependency>\n    <dependency>\n      <groupId>org.springframework.boot</groupId>\n      <artifactId>spring-boot-starter-data-jpa</artifactId>\n    </dependency>\n    <dependency>\n      <groupId>org.postgresql</groupId>\n      <artifactId>postgresql</artifactId>\n      <version>42.6.0</version>\n    </dependency>\n  </dependencies>\n</project>");
            files.put("src/main/java/com/example/User.java", "package com.example;\n\nimport jakarta.persistence.*;\n\n@Entity\n@Table(name = \"users\")\npublic class User {\n    @Id\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\n    private Long id;\n    \n    private String email;\n    private String name;\n    \n    public User() {}\n    public User(String email, String name) {\n        this.email = email;\n        this.name = name;\n    }\n    \n    public Long getId() { return id; }\n    public String getEmail() { return email; }\n    public String getName() { return name; }\n}");
            files.put("src/main/java/com/example/UserRepository.java", "package com.example;\n\nimport org.springframework.data.jpa.repository.JpaRepository;\n\npublic interface UserRepository extends JpaRepository<User, Long> {\n}");
            files.put("src/main/java/com/example/UserController.java", "package com.example;\n\nimport org.springframework.beans.factory.annotation.Autowired;\nimport org.springframework.web.bind.annotation.*;\nimport java.util.List;\n\n@RestController\n@RequestMapping(\"/api/users\")\n@CrossOrigin(origins = \"*\")\npublic class UserController {\n    @Autowired\n    private UserRepository userRepository;\n    \n    @GetMapping\n    public List<User> getAllUsers() {\n        return userRepository.findAll();\n    }\n    \n    @PostMapping\n    public User createUser(@RequestBody User user) {\n        return userRepository.save(user);\n    }\n}");
            files.put("src/main/java/com/example/Application.java", "package com.example;\n\nimport org.springframework.boot.SpringApplication;\nimport org.springframework.boot.autoconfigure.SpringBootApplication;\n\n@SpringBootApplication\npublic class Application {\n    public static void main(String[] args) {\n        SpringApplication.run(Application.class, args);\n    }\n}");
            files.put("src/main/resources/application.properties", "spring.application.name=user-api\nspring.jpa.hibernate.ddl-auto=update\nspring.datasource.url=jdbc:postgresql://localhost:5432/users\nspring.datasource.username=postgres\nspring.datasource.password=password\nserver.port=8080");
            files.put("README.md", "# Spring Boot User API\n\nA production-ready REST API for user management.\n\n## Features\n- CRUD operations for users\n- JPA/Hibernate ORM\n- PostgreSQL database\n- Cross-origin requests enabled\n\n## Getting Started\n\n```bash\nmvn clean install\nmvn spring-boot:run\n```\n\n## API Endpoints\n\n- `GET /api/users` - Get all users\n- `POST /api/users` - Create new user\n\n## License\n\nMIT");
        } else {
            files.put("index.html", "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n    <title>Sample Project</title>\n    <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n    <div class=\"container\">\n        <h1>Hello World</h1>\n        <p>Welcome to your sample project</p>\n    </div>\n    <script src=\"script.js\"></script>\n</body>\n</html>");
            files.put("style.css", "* { margin: 0; padding: 0; box-sizing: border-box; }\nbody { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; }\n.container { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2); text-align: center; max-width: 600px; }\nh1 { color: #333; margin-bottom: 1rem; }\np { color: #666; font-size: 1.1rem; }");
            files.put("script.js", "console.log('Sample project initialized');\ndocument.addEventListener('DOMContentLoaded', function() {\n    console.log('DOM loaded');\n});");
            files.put("README.md", "# Sample Project\n\nA sample HTML/CSS/JavaScript project.\n\n## Getting Started\n\nSimply open `index.html` in your browser.\n\n## Features\n- Responsive design\n- Modern styling\n- Vanilla JavaScript\n\n## License\n\nMIT");
        }
        
        return files;
    }

    private String getFramework(String projectType) {
        if (projectType.toLowerCase().contains("react")) return "react";
        if (projectType.toLowerCase().contains("spring")) return "spring-boot";
        if (projectType.toLowerCase().contains("vue")) return "vue";
        return "generic";
    }

    private String getLanguage(String projectType) {
        if (projectType.toLowerCase().contains("react")) return "typescript";
        if (projectType.toLowerCase().contains("spring")) return "java";
        return "javascript";
    }
}

