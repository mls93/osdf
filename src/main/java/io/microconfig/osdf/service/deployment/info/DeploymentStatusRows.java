package io.microconfig.osdf.service.deployment.info;

import io.microconfig.osdf.healthcheck.HealthcheckerFromFiles;
import io.microconfig.osdf.printers.RowColumnsWithStatus;
import io.microconfig.osdf.service.deployment.ServiceDeployment;
import io.microconfig.osdf.service.files.ServiceFiles;
import io.microconfig.osdf.cluster.pod.Pod;
import io.microconfig.osdf.printers.ColumnPrinter;

import java.util.List;

import static io.microconfig.osdf.service.deployment.info.DeploymentStatus.RUNNING;
import static io.microconfig.osdf.healthcheck.HealthcheckerFromFiles.podsInfo;
import static io.microconfig.utils.ConsoleColor.green;
import static io.microconfig.utils.ConsoleColor.red;
import static java.util.stream.IntStream.range;


public class DeploymentStatusRows implements RowColumnsWithStatus {
    private final ServiceDeployment deployment;
    private final ServiceFiles files;

    private final ColumnPrinter printer;
    private final boolean withHealthCheck;
    private final boolean status;

    public DeploymentStatusRows(ServiceDeployment deployment, ServiceFiles files, ColumnPrinter printer, boolean withHealthCheck) {
        this.deployment = deployment;
        this.files = files;
        this.printer = printer;
        this.withHealthCheck = withHealthCheck;
        this.status = fetch();
    }

    public static DeploymentStatusRows deploymentStatusRows(ServiceDeployment deployment, ServiceFiles files, ColumnPrinter printer, boolean withHealthCheck) {
        return new DeploymentStatusRows(deployment, files, printer, withHealthCheck);
    }

    @Override
    public List<String> getColumns() {
        return printer.getColumns();
    }

    @Override
    public List<List<String>> getRows() {
        return printer.getRows();
    }

    @Override
    public boolean getStatus() {
        return status;
    }

    private boolean fetch() {
        ServiceDeploymentInfo info = deployment.info();
        printer.addRow(green(deployment.serviceName()),
                green(formatVersions(info.version(), deployment.version())),
                green(info.configVersion()),
                coloredStatus(info.status()),
                green(replicas(info)));
        if (withHealthCheck) {
            HealthcheckerFromFiles podsInfo = podsInfo(deployment, files);
            addPods(podsInfo.getPods(), podsInfo.getPodsHealth());
            return podsInfo.isHealthy() && info.status() == RUNNING;
        }
        return info.status() == RUNNING;
    }

    private String formatVersions(String remote, String local) {
        if (remote.equalsIgnoreCase(local)) return remote;
        return remote + " [" + local + "]";
    }

    private String coloredStatus(DeploymentStatus status) {
        String statusString = status.toString().replace("_", " ");
        switch (status) {
            case RUNNING:
                return green(statusString);
            case FAILED:
            case NOT_READY:
            case BAD_HEALTHCHECK:
                return red(statusString);
            default:
                return statusString;
        }
    }

    private void addPods(List<Pod> pods, List<Boolean> podsHealth) {
        range(0, pods.size()).forEach(i -> addPodRow(pods.get(i), podsHealth.get(i)));
    }

    private void addPodRow(Pod pod, boolean health) {
        printer.addRow(" - " + pod.getName(), "", health ? green("OK") : red("BAD"), "");
    }

    private String replicas(ServiceDeploymentInfo info) {
        return info.availableReplicas() + "/" + info.replicas();
    }
}
