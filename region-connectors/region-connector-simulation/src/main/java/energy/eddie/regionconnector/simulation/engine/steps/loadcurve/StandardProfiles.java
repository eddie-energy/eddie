// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.loadcurve;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.*;

public class StandardProfiles {

    private static final StandardProfiles INSTANCE = new StandardProfiles();
    private final Map<String, Profile> profiles = new HashMap<>();

    private StandardProfiles() {
        try {
            parseProfiles();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static StandardProfiles getInstance() {return INSTANCE;}

    public Optional<Profile> getProfile(String name) {
        return Optional.ofNullable(profiles.get(name));
    }

    public Set<String> allProfiles() {
        return profiles.keySet();
    }

    private void parseProfiles() throws IOException {
        try (var inputStream = StandardProfiles.class.getResourceAsStream("/loadcurve/profiles.csv");
             var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))) {

            reader.lines().forEach(line -> {
                var columns = line.split(",", -1);
                var header = columns[0];
                var values = Arrays.stream(columns)
                                   .skip(1)
                                   .filter(String::isBlank)
                                   .map(BigDecimal::new)
                                   .toList();
                profiles.put(header, new Profile(values));
            });
        }
    }
}
