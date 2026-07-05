package org.opentron.backend.controllers;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.opentron.backend.agents.ProjectGenerationService;

/**
 * Project Generation API
 * Generate complete projects (code, configurations, files) via orchestrated agents
 */
@RestController
@RequestMapping("/v1/generate")
public class ProjectGenerationController {

        private static final Logger logger = LoggerFactory.getLogger(ProjectGenerationController.class);

    private final ProjectGenerationService projectGenerator;

    public ProjectGenerationController(ProjectGenerationService projectGenerator) {
        this.projectGenerator = projectGenerator;
    }

    /**
     * POST /v1/generate/project
     * Generate a complete project
     * 
     * Request:
     * {
     *   "name": "UserAuthApp",
     *   "type": "React TypeScript",
     *   "description": "A complete user authentication system with login, signup, password reset"
     * }
     * 
     * Response:
     * {
     *   "name": "UserAuthApp",
     *   "type": "React TypeScript",
     *   "files": {
     *     "src/components/Login.tsx": "...",
     *     "src/components/Signup.tsx": "...",
     *     "src/types/auth.ts": "...",
     *     "package.json": "...",
     *     "README.md": "..."
     *   },
     *   "file_count": 12,
     *   "size_bytes": 45320,
     *   "status": "success",
     *   "elapsed_ms": 3240
     * }
     */
        @PostMapping("/project")
        public Map<String, Object> generateProject(@RequestBody org.opentron.backend.dto.GenerateProjectRequest request) {
                String projectName = request.getName() == null ? "NewProject" : request.getName();
                String projectType = request.getType() == null ? "React TypeScript" : request.getType();
                String description = request.getDescription() == null ? "" : request.getDescription();

                logger.info("Generating project: {}", projectName);

                try {
                        Map<String, Object> result = projectGenerator.generateProject(projectName, description, projectType);
                        return result;
                } catch (Exception e) {
                        logger.error("Error generating project {}", projectName, e);
                        return Map.of(
                                        "status", "error",
                                        "error", e.getMessage(),
                                        "project_name", projectName
                        );
                }
        }

    /**
     * GET /v1/generate/templates
     * List available project templates
     */
    @GetMapping("/templates")
    public Map<String, Object> listTemplates() {
        List<Map<String, Object>> templates = Arrays.asList(
                Map.of(
                        "id", "react-auth",
                        "name", "React Authentication App",
                        "type", "React TypeScript",
                        "description", "Complete user authentication system with login, signup, password reset",
                        "files_generated", 8,
                        "technologies", Arrays.asList("React", "TypeScript", "Tailwind CSS", "API Integration")
                ),
                Map.of(
                        "id", "react-dashboard",
                        "name", "React Admin Dashboard",
                        "type", "React TypeScript",
                        "description", "Full-featured admin dashboard with charts, tables, user management",
                        "files_generated", 12,
                        "technologies", Arrays.asList("React", "TypeScript", "Chart.js", "Redux")
                ),
                Map.of(
                        "id", "springboot-api",
                        "name", "Spring Boot REST API",
                        "type", "Spring Boot Java",
                        "description", "Production-ready REST API with authentication, database, error handling",
                        "files_generated", 10,
                        "technologies", Arrays.asList("Spring Boot", "Java 21", "PostgreSQL", "JWT")
                ),
                Map.of(
                        "id", "nextjs-blog",
                        "name", "Next.js Blog",
                        "type", "Next.js TypeScript",
                        "description", "Full-stack blog with markdown support, comments, dark mode",
                        "files_generated", 15,
                        "technologies", Arrays.asList("Next.js", "TypeScript", "MDX", "Tailwind")
                ),
                Map.of(
                        "id", "fastapi-service",
                        "name", "FastAPI Microservice",
                        "type", "Python FastAPI",
                        "description", "High-performance microservice with async endpoints, validation, docs",
                        "files_generated", 7,
                        "technologies", Arrays.asList("FastAPI", "Python 3.11", "Pydantic", "PostgreSQL")
                )
        );

        return Map.of(
                "templates", templates,
                "count", templates.size(),
                "status", "ok"
        );
    }

    /**
     * POST /v1/generate/from-template
     * Generate project from template
     * 
     * Request:
     * {
     *   "template_id": "react-auth",
     *   "project_name": "MyAuthApp"
     * }
     */
        @PostMapping("/from-template")
        public Map<String, Object> generateFromTemplate(@RequestBody org.opentron.backend.dto.GenerateFromTemplateRequest request) {
                String templateId = request.getTemplate_id() == null ? "react-auth" : request.getTemplate_id();
                String projectName = request.getProject_name() == null ? "NewProject" : request.getProject_name();

                logger.info("Generating from template: {}", templateId);

        // Map template to generation parameters
        Map<String, String> templateMap = Map.ofEntries(
                Map.entry("react-auth", "Complete user authentication system with login, signup, password reset"),
                Map.entry("react-dashboard", "Full-featured admin dashboard with charts, tables, user management"),
                Map.entry("springboot-api", "Production-ready REST API with authentication, database, error handling"),
                Map.entry("nextjs-blog", "Full-stack blog with markdown support, comments, dark mode"),
                Map.entry("fastapi-service", "High-performance microservice with async endpoints, validation, docs")
        );

        String description = templateMap.getOrDefault(templateId, "Project based on template");
        String projectType = templateId.contains("react") ? "React TypeScript"
                : templateId.contains("springboot") ? "Spring Boot Java"
                : templateId.contains("nextjs") ? "Next.js TypeScript"
                : templateId.contains("fastapi") ? "Python FastAPI"
                : "React TypeScript";

        org.opentron.backend.dto.GenerateProjectRequest req = new org.opentron.backend.dto.GenerateProjectRequest(projectName, projectType, description);
        return generateProject(req);
    }

    /**
     * GET /v1/generate/frameworks
     * List supported frameworks
     */
    @GetMapping("/frameworks")
    public Map<String, Object> listFrameworks() {
        Map<String, List<String>> frameworks = Map.ofEntries(
                Map.entry("Frontend", Arrays.asList("React", "Vue", "Next.js", "Svelte", "Angular")),
                Map.entry("Backend", Arrays.asList("Spring Boot", "Node.js Express", "FastAPI", "Django", "Go")),
                Map.entry("Fullstack", Arrays.asList("Next.js", "Nuxt", "Remix", "SvelteKit", "Astro")),
                Map.entry("Mobile", Arrays.asList("React Native", "Flutter", "Ionic", "NativeScript"))
        );

        return Map.of(
                "frameworks", frameworks,
                "status", "ok"
        );
    }

    /**
     * POST /v1/generate/code
     * Generate specific code snippet
     * 
     * Request:
     * {
     *   "request": "Create a React component for a data table with sorting and pagination",
     *   "language": "typescript",
     *   "context": "I'm using Material-UI and React Query"
     * }
     */
        @PostMapping("/code")
        public Map<String, Object> generateCode(@RequestBody org.opentron.backend.dto.GenerateCodeRequest request) {
                String codeRequest = request.getRequest() == null ? "" : request.getRequest();
                String language = request.getLanguage() == null ? "typescript" : request.getLanguage();
                String context = request.getContext() == null ? "" : request.getContext();

                logger.info("Generating code snippet");

                if (codeRequest.isEmpty()) {
                        return Map.of(
                                        "status", "error",
                                        "error", "request parameter is required"
                        );
                }

                try {
                        // Use backend agent for code generation
                        String prompt = String.format("""
                                        Generate clean, production-ready %s code.
                    
                                        Request: %s
                                        Context: %s
                    
                                        Provide:
                                        1. Complete, working code
                                        2. Clear comments
                                        3. Error handling
                                        4. Type hints (if applicable)
                                        5. Usage example
                                        """, language, codeRequest, context);

                        // This would call the backend agent directly
                        return Map.of(
                                        "status", "success",
                                        "language", language,
                                        "request", codeRequest,
                                        "code", "// Generated code would appear here",
                                        "message", "Code generation endpoint ready"
                        );
                } catch (Exception e) {
                        return Map.of(
                                        "status", "error",
                                        "error", e.getMessage()
                        );
                }
        }
}
