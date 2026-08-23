package com.reloop.architecture;

import com.reloop.ReloopApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithArchitectureTest {

    private final ApplicationModules modules = ApplicationModules.of(ReloopApplication.class);

    @Test
    @DisplayName("Verify Spring Modulith bounded contexts and zero illegal cyclic dependencies")
    void verifyModularMonolithStructure() {
        modules.verify();
    }

    @Test
    @DisplayName("Generate Architecture Documentation")
    void generateDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
