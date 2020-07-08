package io.osdf.core.application.local;

import io.osdf.core.application.local.metadata.ApplicationMetadata;
import io.osdf.core.cluster.resource.LocalClusterResource;
import io.osdf.core.local.component.ComponentDir;

import java.util.List;

public interface ApplicationFiles extends ComponentDir {
    List<LocalClusterResource> resources();

    ApplicationMetadata metadata();
}
