package energy.eddie.regionconnector.us.green.button;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

public class XmlLoader {
    public static String xmlFromResource(String fileName) {
        return new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(XmlLoader.class.getResourceAsStream(fileName)),
                        StandardCharsets.UTF_8
                )
        )
                .lines()
                .collect(Collectors.joining("\n"));
    }
}
