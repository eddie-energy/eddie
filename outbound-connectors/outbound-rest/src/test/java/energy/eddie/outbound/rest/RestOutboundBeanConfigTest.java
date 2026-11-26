// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest;

import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.cim.v1_12.vhd.MessageDocumentHeader;
import energy.eddie.cim.v1_12.vhd.MetaInformation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RestOutboundBeanConfigTest {
    private final RestOutboundBeanConfig config = new RestOutboundBeanConfig();

    @Test
    void jsonMapper_omitsNullFields() {
        // Given
        var mapper = config.jsonMapper(List.of(config.jsonMapperBuilderCustomizer()));
        var envelope = new Envelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeader()
                                .withMetaInformation(new MetaInformation().withConnectionId("cid"))
                );

        // When
        var json = mapper.writeValueAsString(envelope);

        // Then
        assertAll(
                () -> assertFalse(json.contains("null"), json),
                () -> assertTrue(json.contains("\"cid\""), json)
        );
    }

    @Test
    void jsonMapper_readsDocumentsWithOmittedFields() {
        // Given
        var mapper = config.jsonMapper(List.of(config.jsonMapperBuilderCustomizer()));
        var envelope = new Envelope()
                .withMessageDocumentHeader(
                        new MessageDocumentHeader()
                                .withMetaInformation(new MetaInformation().withConnectionId("cid"))
                );

        // When
        var res = mapper.readValue(mapper.writeValueAsString(envelope), Envelope.class);

        // Then
        var metaInformation = res.getMessageDocumentHeader().getMetaInformation();
        assertAll(
                () -> assertEquals("cid", metaInformation.getConnectionId()),
                () -> assertNull(metaInformation.getDataNeedId()),
                () -> assertNull(res.getVHDMarketDocument())
        );
    }
}
