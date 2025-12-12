// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.e2etests;

import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

public class PlaywrightOptions implements OptionsFactory {

    public static final String BASE_URL = System.getenv().getOrDefault("E2E_BASE_URL", "http://localhost:8080");
    public static final boolean HEADLESS = !System.getenv().getOrDefault("E2E_HEADLESS", "true").equals("false");

    @Override
    public Options getOptions() {
        return new Options()
                .setTrace(Options.Trace.RETAIN_ON_FAILURE)
                .setBaseUrl(BASE_URL)
                .setChannel("chromium")
                .setHeadless(HEADLESS);
    }
}
