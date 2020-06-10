package io.microconfig.osdf.api.parameter;

import io.microconfig.osdf.parameters.ArgParameter;

import static io.microconfig.osdf.parameters.ParameterUtils.toInteger;

public class CanaryWeightParameter extends ArgParameter<Integer> {
    public CanaryWeightParameter() {
        super("weight", "w", "Traffic weight for canary release. Integer from 0 to 100");
    }

    @Override
    public Integer get() {
        return toInteger(getValue());
    }
}
