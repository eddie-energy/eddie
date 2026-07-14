// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.inbound.ack.cim;

import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.record.InboundRecord;
import energy.eddie.api.agnostic.aiida.AiidaAsset;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xmlunit.builder.DiffBuilder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinMaxEnvelopeAckFormatterStrategyTest {
    private static final UUID AIIDA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DATA_SOURCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final UUID PERMISSION_ID = UUID.fromString("00213495-bdbf-4497-8695-5d811e45aa64");
    private static final UUID DATA_NEED_ID = UUID.fromString("5dc71d7e-e8cd-4403-a3a8-d3c095c97a12");

    private final ClassLoader classLoader = getClass().getClassLoader();
    private final MinMaxEnvelopeAckFormatterStrategy strategy = new MinMaxEnvelopeAckFormatterStrategy(AIIDA_ID);

    @Mock
    private Permission permission;
    @Mock
    private AiidaLocalDataNeed dataNeed;
    @Mock
    private InboundDataSource inboundDataSource;
    @Mock
    private InboundRecord inboundRecord;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var builder = JsonMapper.builder();
        new AiidaConfiguration().objectMapperCustomizer().customize(builder);
        objectMapper = builder.build();

        when(inboundRecord.dataSource()).thenReturn(inboundDataSource);

        when(inboundDataSource.countryCode()).thenReturn("ES");
        when(inboundDataSource.id()).thenReturn(DATA_SOURCE_ID);
        when(inboundDataSource.asset()).thenReturn(AiidaAsset.CONNECTION_AGREEMENT_POINT);
        when(inboundDataSource.meterId()).thenReturn("test-meter-id");
        when(inboundDataSource.operatorId()).thenReturn("test-operator-id");
        when(inboundDataSource.permission()).thenReturn(permission);

        when(permission.id()).thenReturn(PERMISSION_ID);
        when(permission.dataNeed()).thenReturn(dataNeed);

        when(dataNeed.dataNeedId()).thenReturn(DATA_NEED_ID);
    }

    @Test
    void convert_convertsInboundRecordToAcknowledgementEnvelope() throws Exception {
        try (var jsonStream =
                     classLoader.getResourceAsStream("cim/v1_12/min-max-envelope.json")) {

            var payload = new String(
                    Objects.requireNonNull(jsonStream).readAllBytes(),
                    StandardCharsets.UTF_8
            );

            when(inboundRecord.payload()).thenReturn(payload);
            var serde = new XmlMessageSerde();
            var ignoredNames = Set.of(
                    "mRID",
                    "creationDateTime",
                    "createdDateTime"
            );
            var expectedStream = classLoader.getResourceAsStream("cim/v1_12/min-max-envelope.xml");
            assert expectedStream != null;
            var expected = new BufferedReader(new InputStreamReader(expectedStream))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // When
            var envelope = strategy.convert(objectMapper, inboundRecord);

            // Then
            var bytes = serde.serialize(envelope);
            var testXml = new String(bytes, StandardCharsets.UTF_8);
            assertTrue(XmlValidator.validateV112AcknowledgementMarketDocument(bytes));

            var myDiff = DiffBuilder.compare(expected)
                                    .withTest(testXml)
                                    .ignoreWhitespace()
                                    .ignoreComments()
                                    .checkForSimilar()
                                    .withNodeFilter(node -> ignoredNames.stream()
                                                                        .noneMatch(node.getNodeName()::endsWith))
                                    .build();
            assertFalse(myDiff.hasDifferences(), myDiff.fullDescription());
        }
    }
}
