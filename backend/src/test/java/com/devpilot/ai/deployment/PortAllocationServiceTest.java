package com.devpilot.ai.deployment;

import com.devpilot.ai.deployment.service.PortAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PortAllocationServiceTest {

    private PortAllocationService portAllocationService;

    @BeforeEach
    void setUp() {
        portAllocationService = new PortAllocationService();
    }

    @Test
    @DisplayName("Should allocate port in range 10000-11000 and reuse for same deployment ID")
    void testAllocatePort() {
        UUID depId = UUID.randomUUID();

        int port = portAllocationService.allocatePort(depId);
        assertTrue(port >= 10000 && port <= 11000);

        int samePort = portAllocationService.allocatePort(depId);
        assertEquals(port, samePort);
    }

    @Test
    @DisplayName("Should release port and allow reallocation")
    void testReleasePort() {
        UUID depId = UUID.randomUUID();

        int port = portAllocationService.allocatePort(depId);
        assertEquals(Integer.valueOf(port), portAllocationService.getAllocatedPort(depId));

        portAllocationService.releasePort(depId);
        assertNull(portAllocationService.getAllocatedPort(depId));
    }
}
