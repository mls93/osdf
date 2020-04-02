package io.microconfig.osdf.istio.rules;

import io.microconfig.osdf.istio.Destination;
import io.microconfig.osdf.istio.WeightRoute;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.microconfig.osdf.istio.Destination.destination;
import static io.microconfig.osdf.istio.WeightRoute.weightRoute;
import static io.microconfig.osdf.utils.YamlUtils.getList;
import static java.util.List.of;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
public class MainRule {
    private List<WeightRoute> routes;
    private Destination mirror;

    @SuppressWarnings("unchecked")
    public static MainRule fromYaml(Object ruleObject) {
        Map<String, Object> rule = (Map<String, Object>) ruleObject;

        Destination mirror = null;
        if (rule.containsKey("mirror")) {
            mirror = Destination.fromYaml(rule.get("mirror"));
        }
        List<WeightRoute> routes = getList(rule, "route").stream().map(WeightRoute::fromYaml).collect(toList());

        return new MainRule(routes, mirror);
    }

    public void setWeight(String subset, int weight) {
        WeightRoute otherRoute = getOtherRoute(subset);
        Destination destination = destination(otherRoute.getDestination().getHost(), subset);
        routes = of(
                weightRoute(otherRoute.getDestination(), 100 - weight),
                weightRoute(destination, weight)
        ).stream()
                .filter(r -> r.getWeight() != 0)
                .collect(toList());

        mirror = null;
    }

    public void setMirror(String subset) {
        WeightRoute otherRoute = getOtherRoute(subset);
        routes = of (
                weightRoute(otherRoute.getDestination(), 100)
        );
        mirror = destination(otherRoute.getDestination().getHost(), subset);
    }

    public void deleteSubset(String subset) {
        if (mirror != null && mirror.getSubset().equals(subset)) mirror = null;
        List<WeightRoute> otherRoutes = getOtherRoutes(subset);
        if (otherRoutes.size() == 1) {
            routes = of(
                    weightRoute(otherRoutes.get(0).getDestination(), 100)
            );
        } else if (otherRoutes.size() == 0) {
            routes = of();
        }
    }

    public int getWeight(String subset) {
        for (WeightRoute route : routes) {
            if (route.getDestination().getSubset().equals(subset)) {
                return route.getWeight();
            }
        }
        return 0;
    }

    public String mirrorSubset() {
        return mirror == null ? null : mirror.getSubset();
    }

    public boolean isEmpty() {
        return routes.isEmpty();
    }

    private WeightRoute getOtherRoute(String subset) {
        List<WeightRoute> otherRoutes = getOtherRoutes(subset);
        if (otherRoutes.size() != 1) throw new RuntimeException("Only one other version must exist");
        return otherRoutes.get(0);
    }

    private List<WeightRoute> getOtherRoutes(String subset) {
        return routes
                .stream()
                .filter(r -> !r.getDestination().getSubset().equals(subset))
                .collect(toList());
    }

    public Object toYaml() {
        Map<String, Object> route = new HashMap<>();

        route.put("route", routes.stream().map(WeightRoute::toYaml).collect(toList()));
        if (mirror != null) {
            route.put("mirror", mirror.toYaml());
        }
        return route;
    }
}
