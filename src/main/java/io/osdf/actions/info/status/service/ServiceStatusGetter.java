package io.osdf.actions.info.status.service;

import io.osdf.core.application.service.ServiceApplication;
import io.osdf.core.cluster.resource.ClusterResource;
import io.osdf.core.cluster.resource.properties.ResourceProperties;
import io.osdf.core.connection.cli.ClusterCli;
import lombok.RequiredArgsConstructor;

import static io.osdf.actions.info.status.service.ServiceStatus.*;
import static io.osdf.common.utils.StringUtils.castToInteger;
import static io.osdf.core.cluster.resource.properties.ResourceProperties.resourceProperties;
import static java.util.Map.of;

@RequiredArgsConstructor
public class ServiceStatusGetter {
    private final ClusterCli cli;

    public static ServiceStatusGetter serviceStatusGetter(ClusterCli cli) {
        return new ServiceStatusGetter(cli);
    }

    public ServiceStatus statusOf(ServiceApplication service) {
        if (!service.exists()) return NOT_FOUND;
        ClusterResource clusterResource = service.deployment().toResource();
        if (!clusterResource.exists(cli)) return NOT_FOUND;

        ResourceProperties properties = resourceProperties(cli, clusterResource,
                of("replicas", "spec.replicas",
                        "current", "status.replicas",
                        "available", "status.availableReplicas",
                        "unavailable", "status.unavailableReplicas",
                        "ready", "status.readyReplicas"));
        Integer replicas = castToInteger(properties.get("replicas"));
        Integer current = castToInteger(properties.get("current"));
        Integer available = castToInteger(properties.get("available"));
        Integer unavailable = castToInteger(properties.get("unavailable"));
        Integer ready = castToInteger(properties.get("ready"));
        if (replicas == null || unavailable == null || current == null) return FAILED;

        return chooseStatus(replicas, current, available == null ? 0 : available, unavailable, ready == null ? 0 : ready);
    }

    private ServiceStatus chooseStatus(int replicas, int current, int available, int unavailable, int ready) {
        ServiceStatus status = FAILED;
        if (replicas == 0) {
            status = TURNED_OFF;
        } else if (current == ready) {
            status = READY;
        } else if (replicas == available) {
            status = RUNNING;
        } else if (unavailable > 0) {
            status = NOT_READY;
        }
        return status;
    }
}
