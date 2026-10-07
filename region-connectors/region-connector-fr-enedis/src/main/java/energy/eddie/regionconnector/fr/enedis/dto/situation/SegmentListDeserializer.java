// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.situation;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the PRM segments of {@code situation_contrat_auto} regardless of which of the documented
 * candidate shapes Enedis actually returns.
 * <p>
 * The published Swagger leaves the {@code contracts} schema unexpanded, so the shape is not pinned by
 * the spec. Three candidates are known: a bare array ({@code ["C5"]}), an array of segment objects
 * ({@code [{"segment": "C5"}]}, which is how the sibling {@code synth_contrat_auto} API documents the
 * same concept), and a single scalar ({@code "C5"}, which is how the ITC guide renders it as
 * {@code segment: ex. C5, P4}).
 * <p>
 * A type mismatch here would abort the entire master-data fetch, so the permission request would
 * never reach a terminal status. Accepting all documented variants costs little and keeps the DTO
 * correct whichever shape the sandbox returns.
 */
public class SegmentListDeserializer extends ValueDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) {
        return readSegments(p);
    }

    private static List<String> readSegments(JsonParser p) {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            return List.of(unwrap(p.getString()));
        }
        if (token != JsonToken.START_ARRAY) {
            return List.of();
        }

        List<String> segments = new ArrayList<>();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            JsonToken element = p.currentToken();
            switch (element) {
                case JsonToken.VALUE_STRING -> segments.add(unwrap(p.getString()));
                case JsonToken.START_OBJECT -> segments.add(readSegmentObject(p));
                default -> // Unknown element shape: skip it rather than failing the whole response.
                        p.skipChildren();
            }
        }
        return segments;
    }

    private static String readSegmentObject(JsonParser p) {
        String segment = null;
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();
            if ("segment".equals(field) && p.currentToken() == JsonToken.VALUE_STRING) {
                segment = p.getString();
            } else {
                p.skipChildren();
            }
        }
        return segment == null ? "" : unwrap(segment);
    }

    private static String unwrap(String value) {
        return value == null ? "" : value.trim();
    }
}