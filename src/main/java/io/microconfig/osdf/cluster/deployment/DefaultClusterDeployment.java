package io.microconfig.osdf.cluster.deployment;

import io.microconfig.osdf.cluster.cli.ClusterCLI;
import io.microconfig.osdf.cluster.pod.Pod;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.osdf.cluster.pod.Pod.fromOpenShiftNotation;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class DefaultClusterDeployment implements ClusterDeployment {
    private final String name;
    private final String resourceKind;
    private final ClusterCLI cli;

    public static DefaultClusterDeployment defaultClusterDeployment(String name, String resourceKind, ClusterCLI cli) {
        return new DefaultClusterDeployment(name, resourceKind, cli);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<Pod> pods() {
        return cli.execute("get pods " + label() + " -o name")
                .throwExceptionIfError()
                .getOutputLines()
                .stream()
                .filter(line -> line.length() > 0)
                .map(notation -> fromOpenShiftNotation(notation, name, cli))
                .sorted()
                .collect(toUnmodifiableList());
    }

    @Override
    public void scale(int replicas) {
        cli.execute("scale " + resourceKind + " " + name + " --replicas=" + replicas)
                .throwExceptionIfError();
    }

    private String label() {
        String selectorKey = resourceKind.equals("deployment") ? ".spec.selector.matchLabels" : ".spec.selector" ;
        String rawLabelString = cli.execute("get " + resourceKind + " " + name + " -o custom-columns=\"label:" + selectorKey + "\"")
                .throwExceptionIfError()
                .getOutputLines()
                .get(1);
        String labels = of(rawLabelString.strip()
                .substring(4, rawLabelString.length() - 1)
                .split(" "))
                .map(label -> label.split(":"))
                .map(keyValue -> keyValue[0] + " in (" + keyValue[1] + ")")
                .collect(joining(", "));
        return "-l \"" + labels + "\"";
    }
}
