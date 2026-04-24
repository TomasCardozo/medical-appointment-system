package com.tomas.medical.auth.repository;

import com.tomas.medical.auth.entity.Role;
import com.tomas.medical.auth.entity.RoleName;
import com.tomas.medical.auth.entity.User;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.security.jwt.secret=replace-this-with-a-long-secret-key-at-least-32-bytes"
})
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void findByEmailWithRoleReturnsUserAndFetchesRole() {
        Role patientRole = roleRepository.findByName(RoleName.PATIENT).orElseThrow();

        User user = new User();
        user.setFullName("Alice Patient");
        user.setEmail("alice.repo@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole(patientRole);
        user.setActive(true);

        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        User found = userRepository.findByEmailWithRole("alice.repo@example.com").orElseThrow();

        assertThat(found.getEmail()).isEqualTo("alice.repo@example.com");
        assertThat(found.getRole()).isNotNull();
        assertThat(found.getRole().getName()).isEqualTo(RoleName.PATIENT);
        assertThat(Hibernate.isInitialized(found.getRole())).isTrue();
    }

    @Test
    void findByEmailWithRoleReturnsEmptyWhenEmailDoesNotExist() {
        assertThat(userRepository.findByEmailWithRole("missing@example.com")).isEmpty();
    }
}
