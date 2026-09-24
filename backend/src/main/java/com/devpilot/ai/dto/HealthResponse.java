package com.devpilot.ai.dto;

public class HealthResponse {
    private String status;
    private String service;
    private String version;
    private String environment;

    public HealthResponse() {}

    public HealthResponse(String status, String service, String version, String environment) {
        this.status = status;
        this.service = service;
        this.version = version;
        this.environment = environment;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public static class Builder {
        private String status;
        private String service;
        private String version;
        private String environment;

        public Builder status(String status) { this.status = status; return this; }
        public Builder service(String service) { this.service = service; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder environment(String environment) { this.environment = environment; return this; }

        public HealthResponse build() {
            return new HealthResponse(status, service, version, environment);
        }
    }
}
