package com.devpilot.ai.deployment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.ServerSocket;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PortAllocationService {

    private static final Logger log = LoggerFactory.getLogger(PortAllocationService.class);

    private static final int PORT_RANGE_START = 10000;
    private static final int PORT_RANGE_END = 11000;

    private final Map<UUID, Integer> activeAllocations = new ConcurrentHashMap<>();
    private final Set<Integer> allocatedPorts = ConcurrentHashMap.newKeySet();

    public synchronized int allocatePort(UUID deploymentId) {
        if (activeAllocations.containsKey(deploymentId)) {
            return activeAllocations.get(deploymentId);
        }

        for (int port = PORT_RANGE_START; port <= PORT_RANGE_END; port++) {
            if (!allocatedPorts.contains(port) && isPortAvailableOnHost(port)) {
                allocatedPorts.add(port);
                activeAllocations.put(deploymentId, port);
                log.info("Allocated port {} for deployment ID {}", port, deploymentId);
                return port;
            }
        }

        throw new IllegalStateException("No available ports in range " + PORT_RANGE_START + "-" + PORT_RANGE_END);
    }

    public synchronized void releasePort(UUID deploymentId) {
        Integer port = activeAllocations.remove(deploymentId);
        if (port != null) {
            allocatedPorts.remove(port);
            log.info("Released port {} for deployment ID {}", port, deploymentId);
        }
    }

    public Integer getAllocatedPort(UUID deploymentId) {
        return activeAllocations.get(deploymentId);
    }

    private boolean isPortAvailableOnHost(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
