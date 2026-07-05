package org.opentron.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class GenerateFromTemplateRequest {
    @NotBlank
    private String template_id;
    @NotBlank
    private String project_name;

    public GenerateFromTemplateRequest() {}

    public GenerateFromTemplateRequest(String template_id, String project_name) {
        this.template_id = template_id;
        this.project_name = project_name;
    }

    public String getTemplate_id() {
        return template_id;
    }

    public void setTemplate_id(String template_id) {
        this.template_id = template_id;
    }

    public String getProject_name() {
        return project_name;
    }

    public void setProject_name(String project_name) {
        this.project_name = project_name;
    }
}
