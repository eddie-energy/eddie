// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

public class XmlLoader {
    public static String xmlFromResource(String fileName) {
        return new BufferedReader(
                new InputStreamReader(xmlStreamFromResource(fileName), StandardCharsets.UTF_8)
        )
                .lines()
                .collect(Collectors.joining("\n"));
    }

    public static InputStream xmlStreamFromResource(String fileName) {
        return Objects.requireNonNull(XmlLoader.class.getResourceAsStream(fileName));
    }
}
