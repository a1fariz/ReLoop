package com.reloop.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skips tests when a Docker daemon is not reachable (integration tests use
 * Testcontainers-backed infrastructure).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DockerRequired.Check.class)
public @interface DockerRequired {

    class Check implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            try {
                if (DockerClientFactory.instance().isDockerAvailable()) {
                    return ConditionEvaluationResult.enabled("Docker is available");
                }
                return ConditionEvaluationResult.disabled("Docker daemon not reachable");
            } catch (Exception e) {
                return ConditionEvaluationResult.disabled("Docker check failed: " + e.getMessage());
            }
        }
    }
}
