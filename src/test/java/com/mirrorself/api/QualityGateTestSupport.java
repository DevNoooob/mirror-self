package com.mirrorself.api;

import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.ExposedPort;

@Testcontainers
abstract class QualityGateTestSupport {
    @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:0.8.6-pg17").asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("mirror_self_evaluation_qg_v1").withUsername("mirror").withPassword("fictional-qg-only")
        .withLabel("com.mirrorself.test-scope","m2-qg").withLabel("com.mirrorself.eval-run",System.getenv().getOrDefault("MIRROR_QG_RUN_ID","ci-offline"))
        .withCreateContainerCmdModifier(cmd->cmd.getHostConfig().withPortBindings(new PortBinding(Ports.Binding.bindIp("127.0.0.1"),new ExposedPort(5432))));
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",postgres::getJdbcUrl);registry.add("spring.datasource.username",postgres::getUsername);registry.add("spring.datasource.password",postgres::getPassword);
    }
}
