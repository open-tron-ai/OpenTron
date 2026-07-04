package org.opentron.backend.storage.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA Entity for storing generated projects
 */
@Entity
@Table(name = "generated_projects")
public class GeneratedProject {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String projectId;
    
    @Column(nullable = false)
    private String projectName;
    
    @Column(nullable = false)
    private String projectType;
    
    @Column(nullable = false)
    private String framework;
    
    @Column(nullable = false)
    private String language;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Integer fileCount;
    
    @Column(nullable = false)
    private Long sizeBytes;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(columnDefinition = "TEXT")
    private String fileList;
    
    @Column(columnDefinition = "TEXT")
    private String filesJson;
    
    // Constructors
    public GeneratedProject() {}
    
    public GeneratedProject(String projectId, String projectName, String projectType, 
                           String framework, String language, Integer fileCount, Long sizeBytes) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.projectType = projectType;
        this.framework = framework;
        this.language = language;
        this.fileCount = fileCount;
        this.sizeBytes = sizeBytes;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    
    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }
    
    public String getFramework() { return framework; }
    public void setFramework(String framework) { this.framework = framework; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getFileCount() { return fileCount; }
    public void setFileCount(Integer fileCount) { this.fileCount = fileCount; }
    
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getFileList() { return fileList; }
    public void setFileList(String fileList) { this.fileList = fileList; }
    
    public String getFilesJson() { return filesJson; }
    public void setFilesJson(String filesJson) { this.filesJson = filesJson; }
}
