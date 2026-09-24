package com.devpilot.ai.controller;

import com.devpilot.ai.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Administrative endpoints protected by ROLE_ADMIN")
public class AdminTestController {

    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Admin role verification test",
            description = "Accessible only to users with ROLE_ADMIN. Returns 403 Forbidden for ROLE_USER.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> testAdminAccess() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("status", "ACCESS_GRANTED", "scope", "ROLE_ADMIN"),
                "Admin verification successful"
        ));
    }
}
