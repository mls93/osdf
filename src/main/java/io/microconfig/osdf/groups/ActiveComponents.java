package io.microconfig.osdf.groups;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.Components;
import io.microconfig.core.environments.Environment;
import io.microconfig.osdf.configs.ConfigsSettings;
import io.microconfig.osdf.paths.OSDFPaths;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.core.Microconfig.searchConfigsIn;
import static io.microconfig.osdf.settings.SettingsFile.settingsFile;
import static java.util.stream.Collectors.toUnmodifiableList;

@RequiredArgsConstructor
public class ActiveComponents {
    private final List<String> components;

    public static ActiveComponents activeComponents(OSDFPaths paths) {
        ConfigsSettings settings = settingsFile(ConfigsSettings.class, paths.settings().configs()).getSettings();
        String group = settings.getGroup();
        String env = settings.getEnv();

        Environment environment = searchConfigsIn(paths.configsPath().toFile()).inEnvironment(env);
        if (group == null || group.equals("ALL")) {
            return new ActiveComponents(toComponentNames(environment.getAllComponents()));
        }
        return new ActiveComponents(toComponentNames(environment.getGroupWithName(group).getComponents()));
    }

    private static List<String> toComponentNames(Components components) {
        return components
                .asList()
                .stream()
                .map(Component::getName)
                .collect(toUnmodifiableList());
    }

    public List<String> get() {
        return components;
    }
}
