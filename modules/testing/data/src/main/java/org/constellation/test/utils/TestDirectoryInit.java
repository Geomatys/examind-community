package org.constellation.test.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.constellation.configuration.ConfigDirectory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import static org.constellation.test.utils.TestEnvironment.initDataDirectory;

public class TestDirectoryInit implements ApplicationContextInitializer<ConfigurableApplicationContext>, ApplicationListener<ApplicationEvent> {

    static final Logger LOGGER = Logger.getLogger("com.examind.test");
    public static final String DEFAULT_NAME = "TestEnvironment";

    public final String name;

    public TestDirectoryInit() { this(DEFAULT_NAME); }

    public TestDirectoryInit(String name) {
        this.name = name;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        final Path configDir = ConfigDirectory.setupTestEnvironement(name);
        LOGGER.fine(() -> "Configuration directory initialized in " + configDir.toAbsolutePath());
        try {
            final TestEnvironment.TestResources testResources = initDataDirectory();
            LOGGER.fine(() -> "Test resources initialized in " + testResources.outputDir);
            applicationContext.getBeanFactory().registerSingleton(name+".resources", testResources);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot properly initialize data directory", e);
        }
        applicationContext.addApplicationListener(this);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        ConfigDirectory.shutdownTestEnvironement();
        LOGGER.fine(() -> "Configuration directory shutdown for name " + name);
    }
}
