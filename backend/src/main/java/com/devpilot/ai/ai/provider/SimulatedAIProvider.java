package com.devpilot.ai.ai.provider;

import com.devpilot.ai.ai.model.AIRequest;
import com.devpilot.ai.ai.model.AIResponse;
import com.devpilot.ai.ai.model.AIStreamConsumer;
import com.devpilot.ai.ai.model.ChatMessage;
import com.devpilot.ai.entity.enums.AIProviderType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimulatedAIProvider implements AIProvider {

    private static final List<String> MODELS = List.of("devpilot-simulation-v1");

    @Override
    public AIProviderType getProviderType() {
        return AIProviderType.OPENAI; // Serves as local fallback when real keys are unset
    }

    @Override
    public String getDefaultModel() {
        return "devpilot-simulation-v1";
    }

    @Override
    public List<String> getAvailableModels() {
        return MODELS;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public AIResponse generate(AIRequest request) {
        long startTime = System.currentTimeMillis();
        String prompt = request.getMessages().isEmpty() ? "" : request.getMessages().get(request.getMessages().size() - 1).getContent();
        String responseContent = generateSimulationText(prompt);
        long latency = Math.max(120, System.currentTimeMillis() - startTime);

        return new AIResponse(
                responseContent,
                "DEVPILOT_SIMULATOR",
                getDefaultModel(),
                prompt.length() / 4,
                responseContent.length() / 4,
                (prompt.length() + responseContent.length()) / 4,
                latency,
                "stop"
        );
    }

    @Override
    public void generateStream(AIRequest request, AIStreamConsumer consumer) {
        String prompt = request.getMessages().isEmpty() ? "" : request.getMessages().get(request.getMessages().size() - 1).getContent();
        String fullText = generateSimulationText(prompt);

        String[] tokens = fullText.split("(?<=\\s)|(?<=\\n)");
        for (String token : tokens) {
            try {
                Thread.sleep(15);
            } catch (InterruptedException ignored) {}
            consumer.onNext(token);
        }
        consumer.onComplete();
    }

    private String generateSimulationText(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("explain")) {
            return """
                ### Code Analysis & Explanation
                
                1. **High-Level Purpose**:
                   The selected code handles core component logic, structuring execution flow and managing error boundaries.
                   
                2. **Key Mechanisms**:
                   - Parameter validation and state initialization.
                   - Algorithmic evaluation with boundary checking.
                   - Clean return contracts preventing unexpected null pointers.
                   
                3. **Complexity & Considerations**:
                   - Time Complexity: `O(N)` linear traversal.
                   - Space Complexity: `O(1)` auxiliary memory overhead.
                """;
        } else if (lower.contains("fix") || lower.contains("bug")) {
            return """
                ### Bug Diagnosis & Recommended Fix
                
                **Root Cause:**
                A potential boundary condition or null check was missing, which could cause a runtime exception under unexpected inputs.
                
                ```java
                // Corrected implementation with defensive guard clauses
                if (input == null || input.isEmpty()) {
                    return Collections.emptyList();
                }
                return processSafely(input);
                ```
                """;
        } else if (lower.contains("test")) {
            return """
                ### Generated Unit Test Suite
                
                ```java
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.DisplayName;
                import static org.junit.jupiter.api.Assertions.*;

                class ComponentTestSuite {

                    @Test
                    @DisplayName("Should execute successfully with valid parameters")
                    void testValidExecution() {
                        assertDoesNotThrow(() -> {
                            // Verify standard workflow
                        });
                    }

                    @Test
                    @DisplayName("Should handle edge cases and null inputs gracefully")
                    void testEdgeCase() {
                        // Verify boundary resilience
                    }
                }
                ```
                """;
        } else if (lower.contains("refactor")) {
            return """
                ### Refactoring Analysis
                
                **Improvements Made:**
                - Simplified redundant branches into clear modern idioms.
                - Enhanced variable naming and encapsulation.
                
                ```java
                public boolean isValidOperation(String target) {
                    return Optional.ofNullable(target)
                            .filter(t -> !t.isBlank())
                            .isPresent();
                }
                ```
                """;
        }

        return """
            ### DevPilot AI Assistant
            
            I have analyzed your request in the context of your active workspace and file.
            
            ```java
            // Production-ready implementation
            public void executeWorkspaceTask() {
                // Task executed with optimized parameters
            }
            ```
            
            Feel free to inspect the proposed code above and click **Apply** to bring changes directly into your editor buffer.
            """;
    }
}
