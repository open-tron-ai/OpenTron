package org.opentron.backend.controllers;

import java.util.*;
import org.springframework.web.bind.annotation.*;

/**
 * Demo Project Generation
 * Direct code generation without LLM dependency for testing
 */
@RestController
@RequestMapping("/v1/demo")
public class DemoProjectController {

    /**
     * POST /v1/demo/generate-react-auth
     * Generate a complete React authentication app
     */
    @PostMapping("/generate-react-auth")
    public Map<String, Object> generateReactAuth() {
        Map<String, Object> project = new LinkedHashMap<>();

        project.put("name", "AuthApp");
        project.put("type", "React TypeScript");
        project.put("framework", "react");
        project.put("status", "success");

        Map<String, String> files = new LinkedHashMap<>();

        files.put("package.json", """
                {
                  "name": "auth-app",
                  "version": "1.0.0",
                  "type": "module",
                  "scripts": {
                    "dev": "vite",
                    "build": "tsc && vite build",
                    "preview": "vite preview"
                  },
                  "dependencies": {
                    "react": "^18.3.0",
                    "react-dom": "^18.3.0",
                    "react-router-dom": "^6.22.0",
                    "axios": "^1.6.0"
                  },
                  "devDependencies": {
                    "@types/react": "^18.0.0",
                    "@types/react-dom": "^18.0.0",
                    "typescript": "^5.0.0",
                    "vite": "^5.0.0"
                  }
                }
                """);

        files.put("src/main.tsx", """
                import React from 'react'
                import ReactDOM from 'react-dom/client'
                import App from './App'
                import './index.css'
                
                ReactDOM.createRoot(document.getElementById('root')!).render(
                  <React.StrictMode>
                    <App />
                  </React.StrictMode>,
                )
                """);

        files.put("src/App.tsx", """
                import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
                import Login from './pages/Login'
                import Signup from './pages/Signup'
                import Dashboard from './pages/Dashboard'
                import './App.css'
                
                function App() {
                  return (
                    <Router>
                      <Routes>
                        <Route path="/login" element={<Login />} />
                        <Route path="/signup" element={<Signup />} />
                        <Route path="/dashboard" element={<Dashboard />} />
                        <Route path="/" element={<Login />} />
                      </Routes>
                    </Router>
                  )
                }
                
                export default App
                """);

        files.put("src/pages/Login.tsx", """
                import { useState } from 'react'
                import { useNavigate } from 'react-router-dom'
                import axios from 'axios'
                import '../styles/auth.css'
                
                export default function Login() {
                  const [email, setEmail] = useState('')
                  const [password, setPassword] = useState('')
                  const [error, setError] = useState('')
                  const navigate = useNavigate()
                
                  const handleSubmit = async (e: React.FormEvent) => {
                    e.preventDefault()
                    try {
                      const response = await axios.post('/api/auth/login', {
                        email,
                        password,
                      })
                      localStorage.setItem('token', response.data.token)
                      navigate('/dashboard')
                    } catch (err) {
                      setError('Invalid email or password')
                    }
                  }
                
                  return (
                    <div className="auth-container">
                      <div className="auth-card">
                        <h1>Login</h1>
                        {error && <div className="error">{error}</div>}
                        <form onSubmit={handleSubmit}>
                          <input
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                          />
                          <input
                            type="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                          />
                          <button type="submit">Login</button>
                        </form>
                        <p>Don't have an account? <a href="/signup">Sign up</a></p>
                      </div>
                    </div>
                  )
                }
                """);

        files.put("src/pages/Signup.tsx", """
                import { useState } from 'react'
                import { useNavigate } from 'react-router-dom'
                import axios from 'axios'
                import '../styles/auth.css'
                
                export default function Signup() {
                  const [formData, setFormData] = useState({
                    email: '',
                    password: '',
                    confirmPassword: '',
                  })
                  const [error, setError] = useState('')
                  const navigate = useNavigate()
                
                  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
                    setFormData({
                      ...formData,
                      [e.target.name]: e.target.value,
                    })
                  }
                
                  const handleSubmit = async (e: React.FormEvent) => {
                    e.preventDefault()
                    if (formData.password !== formData.confirmPassword) {
                      setError('Passwords do not match')
                      return
                    }
                    try {
                      await axios.post('/api/auth/signup', {
                        email: formData.email,
                        password: formData.password,
                      })
                      navigate('/login')
                    } catch (err) {
                      setError('Failed to create account')
                    }
                  }
                
                  return (
                    <div className="auth-container">
                      <div className="auth-card">
                        <h1>Sign Up</h1>
                        {error && <div className="error">{error}</div>}
                        <form onSubmit={handleSubmit}>
                          <input
                            type="email"
                            name="email"
                            placeholder="Email"
                            value={formData.email}
                            onChange={handleChange}
                            required
                          />
                          <input
                            type="password"
                            name="password"
                            placeholder="Password"
                            value={formData.password}
                            onChange={handleChange}
                            required
                          />
                          <input
                            type="password"
                            name="confirmPassword"
                            placeholder="Confirm Password"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            required
                          />
                          <button type="submit">Sign Up</button>
                        </form>
                        <p>Already have an account? <a href="/login">Login</a></p>
                      </div>
                    </div>
                  )
                }
                """);

        files.put("src/pages/Dashboard.tsx", """
                import { useEffect, useState } from 'react'
                import { useNavigate } from 'react-router-dom'
                import axios from 'axios'
                import '../styles/dashboard.css'
                
                interface User {
                  id: string
                  email: string
                  name: string
                }
                
                export default function Dashboard() {
                  const [user, setUser] = useState<User | null>(null)
                  const navigate = useNavigate()
                
                  useEffect(() => {
                    const token = localStorage.getItem('token')
                    if (!token) {
                      navigate('/login')
                      return
                    }
                
                    const fetchUser = async () => {
                      try {
                        const response = await axios.get('/api/user', {
                          headers: { Authorization: `Bearer ${token}` },
                        })
                        setUser(response.data)
                      } catch (err) {
                        navigate('/login')
                      }
                    }
                
                    fetchUser()
                  }, [navigate])
                
                  const handleLogout = () => {
                    localStorage.removeItem('token')
                    navigate('/login')
                  }
                
                  return (
                    <div className="dashboard">
                      <nav className="navbar">
                        <h1>Dashboard</h1>
                        <button onClick={handleLogout}>Logout</button>
                      </nav>
                      <div className="content">
                        {user && <h2>Welcome, {user.email}!</h2>}
                        <p>This is your dashboard. Customize it as needed.</p>
                      </div>
                    </div>
                  )
                }
                """);

        files.put("src/styles/auth.css", """
                .auth-container {
                  display: flex;
                  justify-content: center;
                  align-items: center;
                  min-height: 100vh;
                  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                }
                
                .auth-card {
                  background: white;
                  padding: 2rem;
                  border-radius: 8px;
                  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
                  width: 100%;
                  max-width: 400px;
                }
                
                .auth-card h1 {
                  text-align: center;
                  margin-bottom: 1.5rem;
                  color: #333;
                }
                
                .auth-card form {
                  display: flex;
                  flex-direction: column;
                  gap: 1rem;
                }
                
                .auth-card input {
                  padding: 0.75rem;
                  border: 1px solid #ddd;
                  border-radius: 4px;
                  font-size: 1rem;
                }
                
                .auth-card input:focus {
                  outline: none;
                  border-color: #667eea;
                  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
                }
                
                .auth-card button {
                  padding: 0.75rem;
                  background: #667eea;
                  color: white;
                  border: none;
                  border-radius: 4px;
                  font-size: 1rem;
                  cursor: pointer;
                  transition: background 0.3s;
                }
                
                .auth-card button:hover {
                  background: #5568d3;
                }
                
                .error {
                  padding: 1rem;
                  background: #fee;
                  color: #c33;
                  border-radius: 4px;
                  margin-bottom: 1rem;
                }
                
                .auth-card p {
                  text-align: center;
                  margin-top: 1rem;
                  color: #666;
                }
                
                .auth-card a {
                  color: #667eea;
                  text-decoration: none;
                }
                """);

        files.put("README.md", """
                # React Authentication App
                
                A complete user authentication system built with React and TypeScript.
                
                ## Features
                
                - User registration (signup)
                - User login with email/password
                - Protected dashboard
                - Session management with JWT tokens
                - Responsive design
                
                ## Getting Started
                
                ### Prerequisites
                - Node.js 18+
                - npm or yarn
                
                ### Installation
                
                ```bash
                npm install
                npm run dev
                ```
                
                ### Build for Production
                
                ```bash
                npm run build
                ```
                
                ## Project Structure
                
                ```
                src/
                ├── pages/
                │   ├── Login.tsx
                │   ├── Signup.tsx
                │   └── Dashboard.tsx
                ├── styles/
                │   ├── auth.css
                │   └── dashboard.css
                ├── App.tsx
                └── main.tsx
                ```
                
                ## API Endpoints Required
                
                - `POST /api/auth/login` - User login
                - `POST /api/auth/signup` - User registration
                - `GET /api/user` - Get current user info
                
                ## License
                
                MIT
                """);

        project.put("files", files);
        project.put("file_count", files.size());
        project.put("size_bytes", files.values().stream().mapToLong(String::length).sum());
        project.put("generated_at", System.currentTimeMillis());
        project.put("elapsed_ms", 250);

        return project;
    }

    /**
     * POST /v1/demo/generate-spring-api
     * Generate a complete Spring Boot REST API
     */
    @PostMapping("/generate-spring-api")
    public Map<String, Object> generateSpringApi() {
        Map<String, Object> project = new LinkedHashMap<>();

        project.put("name", "UserAPI");
        project.put("type", "Spring Boot Java");
        project.put("framework", "spring-boot");
        project.put("status", "success");

        Map<String, String> files = new LinkedHashMap<>();

        files.put("pom.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                         http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>user-api</artifactId>
                    <version>1.0.0</version>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.1.6</version>
                    </parent>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-data-jpa</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.postgresql</groupId>
                            <artifactId>postgresql</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-security</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>io.jsonwebtoken</groupId>
                            <artifactId>jjwt-api</artifactId>
                            <version>0.12.3</version>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-maven-plugin</artifactId>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """);

        files.put("src/main/java/com/example/UserController.java", """
                package com.example;
                
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.*;
                import java.util.List;
                
                @RestController
                @RequestMapping("/api/users")
                @CrossOrigin(origins = "*")
                public class UserController {
                
                    @Autowired
                    private UserRepository userRepository;
                
                    @GetMapping
                    public ResponseEntity<List<User>> getAllUsers() {
                        return ResponseEntity.ok(userRepository.findAll());
                    }
                
                    @GetMapping("/{id}")
                    public ResponseEntity<User> getUserById(@PathVariable Long id) {
                        return userRepository.findById(id)
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
                    }
                
                    @PostMapping
                    public ResponseEntity<User> createUser(@RequestBody User user) {
                        User saved = userRepository.save(user);
                        return ResponseEntity.ok(saved);
                    }
                
                    @PutMapping("/{id}")
                    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
                        return userRepository.findById(id)
                            .map(existing -> {
                                existing.setEmail(user.getEmail());
                                existing.setName(user.getName());
                                return ResponseEntity.ok(userRepository.save(existing));
                            })
                            .orElse(ResponseEntity.notFound().build());
                    }
                
                    @DeleteMapping("/{id}")
                    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
                        userRepository.deleteById(id);
                        return ResponseEntity.noContent().build();
                    }
                }
                """);

        files.put("src/main/java/com/example/User.java", """
                package com.example;
                
                import jakarta.persistence.*;
                import java.time.LocalDateTime;
                
                @Entity
                @Table(name = "users")
                public class User {
                
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    private Long id;
                
                    @Column(unique = true, nullable = false)
                    private String email;
                
                    @Column(nullable = false)
                    private String name;
                
                    @Column(nullable = false)
                    private String password;
                
                    @Column(name = "created_at")
                    private LocalDateTime createdAt = LocalDateTime.now();
                
                    // Constructors, Getters, Setters
                    public User() {}
                
                    public User(String email, String name, String password) {
                        this.email = email;
                        this.name = name;
                        this.password = password;
                    }
                
                    public Long getId() { return id; }
                    public void setId(Long id) { this.id = id; }
                
                    public String getEmail() { return email; }
                    public void setEmail(String email) { this.email = email; }
                
                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }
                
                    public String getPassword() { return password; }
                    public void setPassword(String password) { this.password = password; }
                
                    public LocalDateTime getCreatedAt() { return createdAt; }
                }
                """);

        files.put("src/main/resources/application.yml", """
                spring:
                  application:
                    name: user-api
                  datasource:
                    url: jdbc:postgresql://localhost:5432/user_api
                    username: postgres
                    password: password
                  jpa:
                    hibernate:
                      ddl-auto: update
                    properties:
                      hibernate:
                        dialect: org.hibernate.dialect.PostgreSQLDialect
                
                server:
                  port: 8080
                """);

        files.put("README.md", """
                # Spring Boot User API
                
                A production-ready REST API for user management built with Spring Boot 3.
                
                ## Features
                
                - RESTful API endpoints for user CRUD operations
                - PostgreSQL database integration
                - JWT authentication (ready to integrate)
                - Request validation
                - Error handling
                - Cross-origin support
                
                ## Getting Started
                
                ### Prerequisites
                - Java 21+
                - Maven 3.8+
                - PostgreSQL 12+
                
                ### Installation
                
                ```bash
                git clone <repo>
                cd user-api
                mvn clean install
                mvn spring-boot:run
                ```
                
                ### API Endpoints
                
                - `GET /api/users` - Get all users
                - `GET /api/users/{id}` - Get user by ID
                - `POST /api/users` - Create new user
                - `PUT /api/users/{id}` - Update user
                - `DELETE /api/users/{id}` - Delete user
                
                ### Example Request
                
                ```bash
                curl -X POST http://localhost:8080/api/users \\
                  -H "Content-Type: application/json" \\
                  -d '{"email": "user@example.com", "name": "John Doe", "password": "secure"}'
                ```
                
                ## License
                
                MIT
                """);

        project.put("files", files);
        project.put("file_count", files.size());
        project.put("size_bytes", files.values().stream().mapToLong(String::length).sum());
        project.put("generated_at", System.currentTimeMillis());
        project.put("elapsed_ms", 180);

        return project;
    }
}
