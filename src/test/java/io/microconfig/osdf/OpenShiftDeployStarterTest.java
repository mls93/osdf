package io.microconfig.osdf;

import org.junit.jupiter.api.Test;


class OpenShiftDeployStarterTest {
    @Test
    void testSimpleCall() {
        new OpenShiftDeployStarter(null, null).run(new String[]{"help", "help"});
    }
}