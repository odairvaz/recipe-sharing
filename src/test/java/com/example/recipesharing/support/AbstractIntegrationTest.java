package com.example.recipesharing.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the full application against real PostgreSQL and Redis containers.
 * Skipped (not failed) when Docker is not available.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        // Started once and shared by every integration test class (stopped by Testcontainers' Ryuk on JVM exit).
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGRES.start();
            REDIS.start();
        }
    }
}
