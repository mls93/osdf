package io.microconfig.osdf.develop.service;

import io.microconfig.osdf.cluster.cli.ClusterCLI;
import io.microconfig.osdf.develop.deployment.ClusterDeployment;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static io.microconfig.osdf.develop.deployment.DefaultClusterDeployment.defaultClusterDeployment;
import static io.microconfig.osdf.resources.ResourceVersionInserter.resourceVersionInserter;
import static io.microconfig.osdf.utils.YamlUtils.getString;
import static io.microconfig.osdf.utils.YamlUtils.loadFromPath;

@RequiredArgsConstructor
public class ServiceDeploymentMatcher {
    private final ClusterCLI cli;

    public static ServiceDeploymentMatcher serviceDeploymentMatcher(ClusterCLI cli) {
        return new ServiceDeploymentMatcher(cli);
    }

    public ClusterDeployment match(ServiceFiles files) {
        Map<String, Object> deploy = loadFromPath(files.getPath("deploy.yaml"));
        String version = getString(deploy, "version");
        String serviceType = getString(deploy, "service", "type");
        String deploymentResource = getString(deploy, "deployment" , "resource");
        String deploymentName = deploymentName(files.name(), version, serviceType);
        preprocessIfOldType(files, version, serviceType);
        return defaultClusterDeployment(deploymentName, version, files.name(), deploymentResource, cli);
    }

    private void preprocessIfOldType(ServiceFiles files, String version, String serviceType) {
        if (serviceType.contains("old")) {
            resourceVersionInserter(files.root(), serviceType.contains("istio") ? version : null)
                    .insert();
        }
    }

    private String deploymentName(String name, String version, String serviceType) {
        return serviceType.contains("istio") ? name + "." + version : name;
    }
}
