package ru.practicum.transfer.contract;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

class StubRunnerLocalRepositoryConfig {

    @DynamicPropertySource
    static void configureStubRunnerRepository(DynamicPropertyRegistry registry) {
        Path localRepo = Path.of("target/stubs-repo").toAbsolutePath();
        if (Files.isDirectory(localRepo)) {
            registry.add("stubrunner.repositoryRoot", localRepo::toString);
        } else {
            registry.add("stubrunner.repositoryRoot",
                    () -> Path.of(System.getProperty("user.home"), ".m2", "repository").toString());
        }
    }
}
