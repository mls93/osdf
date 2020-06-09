package io.microconfig.osdf.loadtesting.jmeter;

import io.microconfig.osdf.cluster.pod.Pod;
import io.microconfig.osdf.exceptions.OSDFException;
import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static io.microconfig.osdf.utils.ThreadUtils.sleepSec;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.currentTimeMillis;

@RequiredArgsConstructor
public class JmeterLogResult {
    private final int timeoutInSec;
    private final String finishMarker;
    private final String summaryMarker;

    public static JmeterLogResult jmeterLogResult(int timeoutInSec, String finishMarker, String summaryMarker) {
        return new JmeterLogResult(timeoutInSec, finishMarker, summaryMarker);
    }

    public String getResults(Pod pod) {
        try {
            Process process = getRuntime().exec("oc logs -f " + pod.getName() + " -c " + pod.getComponentName());
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                long startTime = currentTimeMillis();
                StringBuilder logContent = new StringBuilder();
                StringBuilder summary = new StringBuilder();
                boolean gotLogs = false;
                while (true) {
                    if (reader.ready()) {
                        gotLogs = true;
                        String str = reader.readLine();
                        logContent.append(str);
                        checkForSummaryMarker(logContent, summary, str);
                        if (logContent.indexOf(finishMarker) >= 0) {
                            return summary.toString();
                        }
                        cleanLogContent(logContent);
                        continue;
                    }
                    if (!gotLogs && calcSecFrom(startTime) > 10 || calcSecFrom(startTime) > timeoutInSec) {
                        return "Test result hasn't been got";
                    }
                    sleepSec(1);
                }
            }
        } catch (Exception e) {
            throw new OSDFException("Test result hasn't been got. " + e.getMessage());
        }
    }

    private void cleanLogContent(StringBuilder logContent) {
        if (logContent.length() > summaryMarker.length()) {
            logContent.delete(0, logContent.length() - summaryMarker.length());
        }
    }

    private void checkForSummaryMarker(StringBuilder logContent, StringBuilder summary, String str) {
        if (logContent.indexOf(summaryMarker) >= 0) {
            summary.append(str).append("\n");
        }
    }

    private long calcSecFrom(long startTime) {
        return (currentTimeMillis() - startTime) / 1000;
    }
}
