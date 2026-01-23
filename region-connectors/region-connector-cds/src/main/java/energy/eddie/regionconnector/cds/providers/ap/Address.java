// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.ap;

import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

// Regex should be avoided, but there is no address parsing library for american addresses, since this is non-trivial
@SuppressWarnings("java:S5852")
public record Address(
        String houseNumber,
        String street,
        String city,
        String state,
        String zip,
        @Nullable String staircase,
        @Nullable String floor,
        @Nullable String door,
        @Nullable String suffix
) {
    private static final Logger LOGGER = LoggerFactory.getLogger(Address.class);

    @Nullable
    public static Address parse(String input) {
        input = input.trim();
        Function<String, Address> parser = input.contains("\n") ? Address::parseMultiLine : Address::parseSingleLine;
        try {
            return parser.apply(input);
        } catch (Exception e) {
            LOGGER.warn("Could not parse address '{}'", input, e);
        }
        return null;
    }

    private static Address parseSingleLine(String line) {
        var pattern = Pattern.compile("^(\\d+)\\s+(.*?),\\s*([^,]+),\\s*([A-Z]{2})\\s+(\\d{5})(?:-\\d{4})?$");
        var matcher = pattern.matcher(line);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Single-line address is in an invalid format.");
        }

        var houseNumber = matcher.group(1).trim();
        var streetLine = matcher.group(2).trim();
        var city = matcher.group(3).trim();
        var state = matcher.group(4).trim();
        var zip = matcher.group(5).trim();

        return parseExtras(houseNumber, streetLine, city, state, zip);
    }

    private static Address parseMultiLine(String input) {
        var lines = input.split("\\r?\\n", -1);
        if (lines.length < 2) {
            throw new IllegalArgumentException("Incomplete address format.");
        }
        return parseCityStateZip(lines[1].trim(), lines[0].trim());
    }

    private static Address parseCityStateZip(String cityStateZip, String streetLine) {
        var cityStateZipPattern = Pattern.compile("^(.+),\\s*([A-Z]{2})\\s+(\\d{5})(?:-\\d{4})?$");
        var cszMatcher = cityStateZipPattern.matcher(cityStateZip);

        if (!cszMatcher.matches()) {
            throw new IllegalArgumentException("City/State/ZIP line is in an invalid format.");
        }

        var streetPattern = Pattern.compile("^(\\d+)\\s+(.*)$");
        var streetMatcher = streetPattern.matcher(streetLine);
        if (!streetMatcher.matches()) {
            throw new IllegalArgumentException("Street line does not contain a valid house number.");
        }

        var houseNumber = streetMatcher.group(1).trim();
        var street = streetMatcher.group(2).trim();
        var city = cszMatcher.group(1).trim();
        var state = cszMatcher.group(2).trim();
        var zip = cszMatcher.group(3).trim();

        return parseExtras(houseNumber, street, city, state, zip);
    }

    // Sonar wants guarded patterns, but they only work with objects and not string matching
    @SuppressWarnings({"java:S6916", "java:S127", "java:S3776"})
    private static Address parseExtras(String houseNumber, String streetRaw, String city, String state, String zip) {
        String staircase = null;
        String floor = null;
        String door = null;
        String suffix = null;

        // Keywords to look for
        var tokens = streetRaw.split("\\s+", -1);
        var streetBuilder = new StringBuilder();
        var extrasStarted = false;
        List<String> extras = new ArrayList<>();

        for (var token : tokens) {
            if (!extrasStarted && (token.equalsIgnoreCase("Stair")
                                   || token.equalsIgnoreCase("Floor")
                                   || token.equalsIgnoreCase("Door"))) {
                extrasStarted = true;
            }

            if (extrasStarted) {
                extras.add(token);
            } else {
                streetBuilder.append(token).append(" ");
            }
        }

        streetRaw = streetBuilder.toString().trim();

        var inProgress = true;
        for (int i = 0; i < extras.size() && inProgress; i++) {
            var key = extras.get(i).toLowerCase(Locale.ROOT);
            switch (key) {
                case "stair" -> {
                    if (i + 1 < extras.size()) staircase = extras.get(++i);
                }
                case "floor" -> {
                    if (i + 1 < extras.size()) floor = extras.get(++i);
                }
                case "door" -> {
                    if (i + 1 < extras.size()) door = extras.get(++i);
                }
                default -> {
                    suffix = String.join(" ", extras.subList(i, extras.size()));
                    inProgress = false;
                }
            }
        }

        return new Address(houseNumber, streetRaw, city, state, zip, staircase, floor, door, suffix);
    }
}
