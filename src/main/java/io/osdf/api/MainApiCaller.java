package io.osdf.api;

import io.osdf.actions.configs.ConfigsApi;
import io.osdf.actions.info.api.InfoApi;
import io.osdf.actions.init.api.InitializationApi;
import io.osdf.actions.management.ManagementApi;
import io.osdf.actions.system.SystemApi;
import io.osdf.api.lib.ApiCaller;
import io.osdf.api.lib.ApiCallerImpl;
import io.osdf.core.connection.cli.ClusterCli;
import io.osdf.settings.paths.OsdfPaths;
import lombok.RequiredArgsConstructor;
import unstable.io.osdf.ChaosApi;
import unstable.io.osdf.LoadTestingApi;

import java.util.List;

import static io.osdf.actions.configs.ConfigsApiImpl.configsApi;
import static io.osdf.actions.info.api.InfoApiImpl.infoApi;
import static io.osdf.actions.init.api.InitializationApiImpl.initializationApi;
import static io.osdf.actions.management.ManagementApiImpl.managementApi;
import static io.osdf.actions.system.SystemApiImpl.systemApi;
import static io.osdf.api.lib.ApiCallFinder.finder;
import static io.osdf.api.lib.MainApiReader.apiInfo;
import static unstable.io.osdf.ChaosApiImpl.chaosApi;
import static unstable.io.osdf.LoadTestingApiImpl.loadTestingApi;

@RequiredArgsConstructor
public class MainApiCaller implements ApiCaller {
    private final OsdfPaths paths;
    private final ClusterCli cli;

    public static ApiCaller mainApi(OsdfPaths paths, ClusterCli cli) {
        return new MainApiCaller(paths, cli);
    }

    @Override
    public void call(List<String> args) {
        if (args.isEmpty()) {
            apiInfo(MainApi.class).printHelp();
            return;
        }
        ApiCallerImpl.builder()
                .finder(finder(MainApi.class))
                .addImpl(InitializationApi.class, initializationApi(paths, cli))
                .addImpl(ConfigsApi.class, configsApi(paths, cli))
                .addImpl(ManagementApi.class, managementApi(paths, cli))
                .addImpl(InfoApi.class, infoApi(paths, cli))
                .addImpl(SystemApi.class, systemApi(paths))
                .addImpl(ChaosApi.class, chaosApi(paths, cli))
                .addImpl(LoadTestingApi.class, loadTestingApi(paths, cli))
                .build()
                .call(args);
    }
}
