// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.e2etests;

import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

import java.nio.file.Path;

public class PlaywrightOptions implements OptionsFactory {

    public static final String EDDIE_URL = System.getenv().getOrDefault("E2E_EDDIE_URL", "http://localhost:8080");
    public static final String AIIDA_URL = System.getenv().getOrDefault("E2E_AIIDA_URL", "http://localhost:8081");
    public static final String ADMIN_URL = System.getenv().getOrDefault("E2E_ADMIN_URL",
                                                                        "http://localhost:9090/outbound-connectors/admin-console");
    public static final boolean HEADLESS = !System.getenv().getOrDefault("E2E_HEADLESS", "true").equals("false");

    @Override
    public Options getOptions() {
        return new Options()
                .setTrace(Options.Trace.RETAIN_ON_FAILURE)
                .setOutputDir(Path.of("build", "test-results", "test", "traces"))
                .setChannel("chromium")
                .setHeadless(HEADLESS);
    }
}
