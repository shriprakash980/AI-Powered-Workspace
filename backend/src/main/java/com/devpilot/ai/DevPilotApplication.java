package com.devpilot.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class DevPilotApplication {

    private static final Logger log = LoggerFactory.getLogger(DevPilotApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DevPilotApplication.class);
        Environment env = app.run(args).getEnvironment();
        String port = env.getProperty("server.port", "8080");
        log.info("==================================================================");
        log.info("🚀 DevPilot AI Backend running at: http://localhost:{}", port);
        log.info("📖 Swagger UI Documentation: http://localhost:{}/swagger-ui/index.html", port);
        log.info("🩺 Health Check Endpoint: http://localhost:{}/api/v1/health", port);
        log.info("==================================================================");
    }
}
