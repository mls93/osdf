package io.microconfig.osdf.api.implementations;

import io.osdf.core.connection.cli.ClusterCli;
import io.osdf.core.connection.cli.openshift.OpenShiftCli;
import io.osdf.common.Credentials;
import io.osdf.common.exceptions.OSDFException;
import io.osdf.common.utils.TestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static io.osdf.actions.init.InitializationApiImpl.initializationApi;
import static io.osdf.common.utils.TestContext.CONFIGS_PATH;
import static io.osdf.common.utils.TestContext.defaultContext;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.list;
import static java.util.Comparator.naturalOrder;
import static java.util.List.of;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class InitializationApiImplTest {
    private static final TestContext context = defaultContext();
    private final ClusterCli cli = mock(ClusterCli.class);

    @BeforeEach
    void prepareEnv() throws IOException {
        context.install();
        context.prepareConfigs();
    }

    @Test
    void initLocalConfigs() {
        initializationApi(context.getPaths(), cli).localConfigs(CONFIGS_PATH, "master");
        assertTrue(exists(context.getPaths().configsPath()));
    }

    @Test
    void exceptionIfPathIsNotProvided() {
        assertThrows(OSDFException.class, () -> initializationApi(context.getPaths(), mock(OpenShiftCli.class)).localConfigs(null, null));
    }

    @Test
    void buildIfEnvIsSet() throws IOException {
        initializationApi(context.getPaths(), cli).localConfigs(CONFIGS_PATH, null);
        initializationApi(context.getPaths(), cli).configs("dev", null);
        try (Stream<Path> files = list(context.getPaths().componentsPath())) {
            List<String> builtComponents = files.map(Path::getFileName)
                    .map(Path::toString)
                    .sorted(naturalOrder())
                    .collect(toUnmodifiableList());
            assertEquals(of("simple-job", "simple-service"), builtComponents);
        }
    }

    @Test
    void exceptionIfEnvIsNotProvided() {
        initializationApi(context.getPaths(), cli).localConfigs(CONFIGS_PATH, null);
        assertThrows(OSDFException.class, () -> initializationApi(context.getPaths(), cli).configs(null, null));
    }

    @Test
    void exceptionIfBuildWithoutConfigs() {
        assertThrows(OSDFException.class, () -> initializationApi(context.getPaths(), cli).configs("dev", null));
    }

    @Test
    void exceptionIfNoArgsForOpenShiftInit() {
        assertThrows(OSDFException.class, () -> initializationApi(context.getPaths(), cli).openshift(null, null, false));
    }

    @Test
    void exceptionIfBothArgsForOpenShiftInit() {
        assertThrows(OSDFException.class, () -> initializationApi(context.getPaths(), cli).openshift(Credentials.of("user:pass"), "token", false));
    }

    @Test
    void initOpenShift() {
        initializationApi(context.getPaths(), cli).openshift(Credentials.of("user:pass"), null, false);
        exists(context.getPaths().settings().openshift());

        initializationApi(context.getPaths(), cli).openshift(null, "token", false);
        exists(context.getPaths().settings().openshift());
    }
}