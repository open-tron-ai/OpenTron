package org.opentron.backend.controllers;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.opentron.backend.agents.ProjectGenerationService;

/**
 * Project Download API
 * Download generated projects as ZIP files
 */
@RestController
@RequestMapping("/v1/download")
public class ProjectDownloadController {

    private final ProjectGenerationService projectGenerator;

    public ProjectDownloadController(ProjectGenerationService projectGenerator) {
        this.projectGenerator = projectGenerator;
    }

    /**
     * GET /v1/download/react-auth
     * Download React authentication app as ZIP
     */
    @GetMapping("/react-auth")
    public ResponseEntity<byte[]> downloadReactAuth() throws IOException {
        Map<String, Object> project = getReactAuthProject();
        byte[] zipBytes = createZipFromProject(project);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"auth-app.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }

    /**
     * GET /v1/download/spring-api
     * Download Spring Boot API as ZIP
     */
    @GetMapping("/spring-api")
    public ResponseEntity<byte[]> downloadSpringApi() throws IOException {
        Map<String, Object> project = getSpringApiProject();
        byte[] zipBytes = createZipFromProject(project);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"user-api.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipBytes);
    }

    /**
     * Create ZIP from project files
     */
    private byte[] createZipFromProject(Map<String, Object> project) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, String> files = (Map<String, String>) project.get("files");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, String> file : files.entrySet()) {
                ZipEntry entry = new ZipEntry(file.getKey());
                zos.putNextEntry(entry);
                zos.write(file.getValue().getBytes("UTF-8"));
                zos.closeEntry();
            }
        }

        return baos.toByteArray();
    }

    /**
     * Get React auth project structure
     */
    private Map<String, Object> getReactAuthProject() {
        Map<String, Object> project = new LinkedHashMap<>();
        project.put("name", "auth-app");

        Map<String, String> files = new LinkedHashMap<>();

        files.put("package.json", "{\n  \"name\": \"auth-app\",\n  \"version\": \"1.0.0\",\n  \"type\": \"module\",\n  \"scripts\": {\n    \"dev\": \"vite\",\n    \"build\": \"tsc && vite build\",\n    \"preview\": \"vite preview\"\n  },\n  \"dependencies\": {\n    \"react\": \"^18.3.0\",\n    \"react-dom\": \"^18.3.0\",\n    \"react-router-dom\": \"^6.22.0\",\n    \"axios\": \"^1.6.0\"\n  },\n  \"devDependencies\": {\n    \"@types/react\": \"^18.0.0\",\n    \"@types/react-dom\": \"^18.0.0\",\n    \"typescript\": \"^5.0.0\",\n    \"vite\": \"^5.0.0\"\n  }\n}");

        files.put("src/main.tsx", "import React from 'react'\nimport ReactDOM from 'react-dom/client'\nimport App from './App'\nimport './index.css'\n\nReactDOM.createRoot(document.getElementById('root')!).render(\n  <React.StrictMode>\n    <App />\n  </React.StrictMode>,\n)");

        files.put("src/App.tsx", "import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'\nimport Login from './pages/Login'\nimport Signup from './pages/Signup'\nimport Dashboard from './pages/Dashboard'\nimport './App.css'\n\nfunction App() {\n  return (\n    <Router>\n      <Routes>\n        <Route path=\"/login\" element={<Login />} />\n        <Route path=\"/signup\" element={<Signup />} />\n        <Route path=\"/dashboard\" element={<Dashboard />} />\n        <Route path=\"/\" element={<Login />} />\n      </Routes>\n    </Router>\n  )\n}\n\nexport default App");

        files.put("src/pages/Login.tsx", "import { useState } from 'react'\nimport { useNavigate } from 'react-router-dom'\nimport axios from 'axios'\nimport '../styles/auth.css'\n\nexport default function Login() {\n  const [email, setEmail] = useState('')\n  const [password, setPassword] = useState('')\n  const [error, setError] = useState('')\n  const navigate = useNavigate()\n\n  const handleSubmit = async (e: React.FormEvent) => {\n    e.preventDefault()\n    try {\n      const response = await axios.post('/api/auth/login', {\n        email,\n        password,\n      })\n      localStorage.setItem('token', response.data.token)\n      navigate('/dashboard')\n    } catch (err) {\n      setError('Invalid email or password')\n    }\n  }\n\n  return (\n    <div className=\"auth-container\">\n      <div className=\"auth-card\">\n        <h1>Login</h1>\n        {error && <div className=\"error\">{error}</div>}\n        <form onSubmit={handleSubmit}>\n          <input\n            type=\"email\"\n            placeholder=\"Email\"\n            value={email}\n            onChange={(e) => setEmail(e.target.value)}\n            required\n          />\n          <input\n            type=\"password\"\n            placeholder=\"Password\"\n            value={password}\n            onChange={(e) => setPassword(e.target.value)}\n            required\n          />\n          <button type=\"submit\">Login</button>\n        </form>\n        <p>Don't have an account? <a href=\"/signup\">Sign up</a></p>\n      </div>\n    </div>\n  )\n}");

        files.put("README.md", "# React Authentication App\n\nA complete user authentication system built with React and TypeScript.\n\n## Features\n\n- User registration (signup)\n- User login with email/password\n- Protected dashboard\n- Session management with JWT tokens\n- Responsive design\n\n## Getting Started\n\n### Prerequisites\n- Node.js 18+\n- npm or yarn\n\n### Installation\n\n```bash\nnpm install\nnpm run dev\n```\n\n## License\n\nMIT");

        project.put("files", files);
        return project;
    }

    /**
     * Get Spring Boot API project structure
     */
    private Map<String, Object> getSpringApiProject() {
        Map<String, Object> project = new LinkedHashMap<>();
        project.put("name", "user-api");

        Map<String, String> files = new LinkedHashMap<>();

        files.put("pom.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>com.example</groupId>\n  <artifactId>user-api</artifactId>\n  <version>1.0.0</version>\n  <parent>\n    <groupId>org.springframework.boot</groupId>\n    <artifactId>spring-boot-starter-parent</artifactId>\n    <version>3.1.6</version>\n  </parent>\n  <dependencies>\n    <dependency>\n      <groupId>org.springframework.boot</groupId>\n      <artifactId>spring-boot-starter-web</artifactId>\n    </dependency>\n    <dependency>\n      <groupId>org.springframework.boot</groupId>\n      <artifactId>spring-boot-starter-data-jpa</artifactId>\n    </dependency>\n    <dependency>\n      <groupId>org.postgresql</groupId>\n      <artifactId>postgresql</artifactId>\n    </dependency>\n  </dependencies>\n</project>");

        files.put("src/main/java/com/example/UserController.java", "package com.example;\n\nimport org.springframework.beans.factory.annotation.Autowired;\nimport org.springframework.http.ResponseEntity;\nimport org.springframework.web.bind.annotation.*;\nimport java.util.List;\n\n@RestController\n@RequestMapping(\"/api/users\")\n@CrossOrigin(origins = \"*\")\npublic class UserController {\n\n  @Autowired\n  private UserRepository userRepository;\n\n  @GetMapping\n  public ResponseEntity<List<User>> getAllUsers() {\n    return ResponseEntity.ok(userRepository.findAll());\n  }\n\n  @PostMapping\n  public ResponseEntity<User> createUser(@RequestBody User user) {\n    User saved = userRepository.save(user);\n    return ResponseEntity.ok(saved);\n  }\n}");

        files.put("README.md", "# Spring Boot User API\n\nA production-ready REST API for user management.\n\n## Getting Started\n\n```bash\nmvn clean install\nmvn spring-boot:run\n```\n\n## API Endpoints\n\n- `GET /api/users` - Get all users\n- `POST /api/users` - Create new user\n\n## License\n\nMIT");

        project.put("files", files);
        return project;
    }
}
