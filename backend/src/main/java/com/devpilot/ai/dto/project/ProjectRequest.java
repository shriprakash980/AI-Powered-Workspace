package com.devpilot.ai.dto.project;

import com.devpilot.ai.entity.enums.ProjectTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 100, message = "Project name must be between 2 and 100 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Template is required")
    private ProjectTemplate template;

    @NotBlank(message = "Programming language is required")
    private String language;

    private String framework;

    public ProjectRequest() {}

    public ProjectRequest(String name, String description, ProjectTemplate template, String language, String framework) {
        this.name = name;
        this.description = description;
        this.template = template;
        this.language = language;
        this.framework = framework;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProjectTemplate getTemplate() { return template; }
    public void setTemplate(ProjectTemplate template) { this.template = template; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getFramework() { return framework; }
    public void setFramework(String framework) { this.framework = framework; }

    public static class Builder {
        private String name;
        private String description;
        private ProjectTemplate template;
        private String language;
        private String framework;

        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder template(ProjectTemplate template) { this.template = template; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder framework(String framework) { this.framework = framework; return this; }

        public ProjectRequest build() {
            return new ProjectRequest(name, description, template, language, framework);
        }
    }
}
