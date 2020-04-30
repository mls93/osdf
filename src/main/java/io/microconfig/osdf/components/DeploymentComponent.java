package io.microconfig.osdf.components;

import io.microconfig.osdf.components.info.DeploymentInfo;
import io.microconfig.osdf.exceptions.OSDFException;
import io.microconfig.osdf.openshift.OCExecutor;
import io.microconfig.osdf.openshift.Pod;
import io.microconfig.osdf.paths.OSDFPaths;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

import static io.microconfig.osdf.components.loader.ComponentsLoaderImpl.componentsLoader;
import static io.microconfig.osdf.components.properties.DeployProperties.deployProperties;
import static io.microconfig.osdf.istio.VirtualService.virtualService;
import static io.microconfig.osdf.openshift.Pod.fromOpenShiftNotation;
import static java.util.List.of;
import static java.util.stream.Collectors.toUnmodifiableList;

@Getter
public class DeploymentComponent extends AbstractOpenShiftComponent {
    private final boolean istioService;

    public DeploymentComponent(String name, String version, Path configDir, OCExecutor oc) {
        super(name, version, configDir, oc);
        this.istioService = "istio".equals(deployProperties(configDir).getType());
    }

    public static DeploymentComponent component(String name, OSDFPaths paths, OCExecutor oc) {
        DeploymentComponent component = componentsLoader(paths, of(name), oc)
                .load(DeploymentComponent.class)
                .stream()
                .findFirst()
                .orElse(null);
        if (component == null) throw new OSDFException("Component " + name + " not found");
        return component;
    }

    public static DeploymentComponent fromNotation(String notation, Path configDir, OCExecutor oc) {
        String fullName = notation.split("/")[1];
        String name = fullName.split("\\.")[0];
        String version = fullName.substring(name.length() + 1);
        return new DeploymentComponent(name, version, configDir, oc);
    }

    @Override
    public void delete() {
        if (istioService) {
            virtualService(oc, this).deleteRules();
        }
        super.delete();
    }

    @Override
    public void deleteAll() {
        virtualService(oc, this).delete();
        super.deleteAll();
    }

    public void stop() {
        scale(0);
    }


    public void restart() {
        int replicas = info().getReplicas();
        if (replicas > 0) {
            scale(0);
            scale(replicas);
        } else {
            upload();
        }
    }

    public List<Pod> pods() {
        return oc.executeAndReadLines("oc get pods " + label() + " -o name")
                .stream()
                .filter(line -> line.length() > 0)
                .map(notation -> fromOpenShiftNotation(notation, name, oc))
                .sorted()
                .collect(toUnmodifiableList());
    }

    public boolean isDeployed() {
        return !oc.execute("oc get dc " + fullName(), true).contains("not found");
    }

    public boolean isRunning() {
        DeploymentInfo info = info();
        return info.getReplicas() == info.getAvailableReplicas() && info.getAvailableReplicas() > 0;
    }

    public DeploymentInfo info() {
        return DeploymentInfo.info(this, oc);
    }

    public List<DeploymentComponent> getDeployedComponents() {
        return oc.executeAndReadLines("oc get dc -l application=" + name + " -o name")
                .stream()
                .filter(line -> line.length() > 0)
                .map(notation -> fromNotation(notation, configDir, oc))
                .collect(toUnmodifiableList());
    }

    private void scale(int replicas) {
        oc.execute("oc scale dc " + fullName() + " --replicas=" + replicas);
    }
}
