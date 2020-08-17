package io.osdf.common.yaml;

import io.osdf.common.exceptions.OSDFException;
import org.junit.jupiter.api.Test;

import static io.osdf.common.yaml.YamlObject.yaml;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YamlObjectTest {
    @Test
    void ifContentIsNotValidYaml_throwOsdfException() {
        assertThrows(OSDFException.class, () -> yaml("a: b\n- c"));
    }
}