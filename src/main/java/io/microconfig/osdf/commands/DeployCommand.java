package io.microconfig.osdf.commands;

import io.microconfig.osdf.cluster.cli.ClusterCLI;
import io.microconfig.osdf.deployers.ServiceDeployer;
import io.microconfig.osdf.exceptions.OSDFException;
import io.microconfig.osdf.jobrunners.DefaultJobRunner;
import io.microconfig.osdf.paths.OSDFPaths;
import io.microconfig.osdf.service.deployment.pack.ServiceDeployPack;
import io.microconfig.osdf.service.job.pack.ServiceJobPack;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.osdf.deployers.BaseServiceDeployer.baseServiceDeployer;
import static io.microconfig.osdf.deployers.RestrictedDeployer.restrictedDeployer;
import static io.microconfig.osdf.jobrunners.DefaultJobRunner.defaultJobRunner;
import static io.microconfig.osdf.resources.DeploymentHashInserter.deploymentHashInserter;
import static io.microconfig.osdf.service.deployment.checkers.DeployStatusChecker.deployStatusChecker;
import static io.microconfig.osdf.service.deployment.pack.loader.DefaultServiceDeployPacksLoader.serviceLoader;
import static io.microconfig.osdf.service.deployment.tools.DeployRequiredFilter.deployRequiredFilter;
import static io.microconfig.osdf.service.job.pack.loader.DefaultServiceJobPackLoader.jobLoader;
import static io.microconfig.osdf.service.loaders.filters.RequiredComponentsFilter.requiredComponentsFilter;
import static io.microconfig.utils.Logger.announce;
import static io.microconfig.utils.Logger.error;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

@RequiredArgsConstructor
public class DeployCommand {
    private final OSDFPaths paths;
    private final ClusterCLI cli;

    public static DeployCommand deployCommand(OSDFPaths paths, ClusterCLI cli) {
        return new DeployCommand(paths, cli);
    }

    public List<ServiceDeployPack> deploy(List<String> serviceNames, String mode, boolean wait) {
        ServiceDeployer deployer = getDeployer(mode);
        announce("Starting deployment");

        List<ServiceJobPack> jobPacks = jobLoader(paths, serviceNames, cli).loadPacks();
        callRunner(jobPacks);

        List<ServiceDeployPack> deployPacks = getDeployPacks(serviceNames, mode);
        if (deployPacks.isEmpty()) return emptyList();

        callDeployer(deployPacks, deployer);
        if (wait && !"restricted".equals(mode)) {
            announce("Waiting for services to deploy");
            return findFailedDeployments(deployPacks);
        }
        return emptyList();
    }

    private List<ServiceDeployPack> getDeployPacks(List<String> serviceNames, String mode) {
        List<ServiceDeployPack> allPacks = serviceLoader(paths, requiredComponentsFilter(serviceNames), cli).loadPacks();
        List<ServiceDeployPack> deployPacks = "restricted".equals(mode) ? allPacks : deployRequiredFilter(paths, cli).filter(allPacks);
        if ("restricted".equals(mode)) {
            allPacks.forEach(pack -> deploymentHashInserter().insert(pack.files()));
        }
        if (deployPacks.isEmpty())  {
            announce("No services to deploy");
        } else {
            announce("Deploying: " +
                    deployPacks.stream()
                            .map(deployPack -> deployPack.service().name())
                            .collect(joining(" ")));
        }
        return deployPacks;
    }

    private List<ServiceDeployPack> findFailedDeployments(List<ServiceDeployPack> deployPacks) {
        List<ServiceDeployPack> failedDeployments = deployStatusChecker().findFailed(deployPacks);
        if (failedDeployments.isEmpty()) {
            announce("OK");
        } else {
            error("Some components didn't start in time or had failures");
        }
        return failedDeployments;
    }

    private void callDeployer(List<ServiceDeployPack> deployPacks, ServiceDeployer deployer) {
        range(0, deployPacks.size()).forEach(i -> deployer.deploy(
                        deployPacks.get(i).service(),
                        deployPacks.get(i).deployment(),
                        deployPacks.get(i).files())
        );
    }

    private ServiceDeployer getDeployer(String mode) {
        if (mode == null) {
            return baseServiceDeployer(cli);
        }
        if (mode.equals("restricted")) {
            return restrictedDeployer();
        }
        throw new OSDFException("Unknown deploy mode");
    }

    private void callRunner(List<ServiceJobPack> jobPacks) {
        DefaultJobRunner runner = defaultJobRunner();
        range(0, jobPacks.size()).forEach(i -> runner.run(
                jobPacks.get(i).service(),
                jobPacks.get(i).job(),
                jobPacks.get(i).files())
        );
    }
}
