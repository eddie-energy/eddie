package energy.eddie.outbound.rest.web;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataMarketDocumentComplexType;
import energy.eddie.outbound.rest.RestOutboundBeanConfig;
import energy.eddie.outbound.rest.TestDataSourceInformation;
import energy.eddie.outbound.rest.dto.ConnectionStatusMessages;
import energy.eddie.outbound.rest.dto.ValidatedHistoricalDataMarketDocuments;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class FallbackXmlMessageConverterTest {
    private final FallbackXmlMessageConverter converter = new FallbackXmlMessageConverter(
            new MarshallingHttpMessageConverter(new RestOutboundBeanConfig().jaxb2Marshaller()),
            new MappingJackson2XmlHttpMessageConverter()
    );

    @ParameterizedTest
    @MethodSource("cimAndAgnosticClasses")
    void canRead_forAgnosticAndCimClasses_returnsTrue(Class<?> clazz) {
        // Given
        // When
        var res = converter.canRead(clazz, MediaType.APPLICATION_XML);

        // Then
        assertTrue(res);
    }

    @ParameterizedTest
    @MethodSource("cimAndAgnosticClasses")
    void canWrite_forAgnosticAndCimClasses_returnsTrue(Class<?> clazz) {
        // Given
        // When
        var res = converter.canWrite(clazz, MediaType.APPLICATION_XML);

        // Then
        assertTrue(res);
    }

    @Test
    // False positive by sonar
    @SuppressWarnings("java:S1612")
    void getSupportedMediaTypes_doesNotThrow() {
        // Given & When & Then
        assertDoesNotThrow(() -> converter.getSupportedMediaTypes());
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void read_validatedHistoricalDataMarketDocuments_returnsHistoricalDataMarketDocuments() throws IOException {
        // Given
        var xml = getClass().getResourceAsStream("/vhds.xml");
        var msg = new MockHttpInputMessage(xml);

        // When
        var res = converter.read(ValidatedHistoricalDataMarketDocuments.class, msg);

        // Then
        assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(ValidatedHistoricalDataMarketDocuments.class))
                .extracting(ValidatedHistoricalDataMarketDocuments::getDocuments)
                .asInstanceOf(InstanceOfAssertFactories.list(ValidatedHistoricalDataEnvelope.class))
                .singleElement()
                .extracting(ValidatedHistoricalDataEnvelope::getValidatedHistoricalDataMarketDocument)
                .extracting(ValidatedHistoricalDataMarketDocumentComplexType::getMRID)
                .isEqualTo("3847de2f-45f8-44e8-8a68-591635477f94");
    }

    @Test
    void read_connectionStatusMessage_throws() {
        // Given
        // language=XML
        var xml = """
                <ConnectionStatusMessages>
                    <ConnectionStatusMessage>
                        <connectionId>1</connectionId>
                        <permissionId>11162130-a49f-4705-aa1a-3165c725f69f</permissionId>
                        <dataNeedId>9bd0668f-cc19-40a8-99db-dc2cb2802b17</dataNeedId>
                        <dataSourceInformation>
                            <countryCode>DE</countryCode>
                            <meteredDataAdministratorId>sim</meteredDataAdministratorId>
                            <permissionAdministratorId>sim</permissionAdministratorId>
                            <regionConnectorId>sim</regionConnectorId>
                        </dataSourceInformation>
                        <timestamp>2025-07-23T06:09:19.066591086Z</timestamp>
                        <status>CREATED</status>
                        <message></message>
                        <additionalInformation/>
                    </ConnectionStatusMessage>
                </ConnectionStatusMessages>
                """;
        var msg = new MockHttpInputMessage(xml.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThrows(HttpMessageNotReadableException.class, () -> converter.read(ConnectionStatusMessages.class, msg));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    void write_validatedHistoricalDataMarketDocuments_returnsHistoricalDataMarketDocuments() throws IOException {
        // Given
        var xml = getClass().getResourceAsStream("/vhds.xml");
        var vhds = (ValidatedHistoricalDataMarketDocuments) converter.read(ValidatedHistoricalDataMarketDocuments.class,
                                                                           new MockHttpInputMessage(xml));
        var vhd = vhds.getDocuments().getFirst();
        var msg = new MockHttpOutputMessage();

        // When
        converter.write(vhd, MediaType.APPLICATION_XML, msg);

        // Then
        var res = msg.getBodyAsString();
        assertTrue(XmlValidator.validateValidatedHistoricalMarketDocument(res));
    }

    @Test
    void write_connectionStatusMessage_doesNotThrow() {
        // Given
        var csm = new ConnectionStatusMessages(List.of(
                new ConnectionStatusMessage(
                        "cid",
                        "pid",
                        "dnid",
                        new TestDataSourceInformation("AT", "at-eda", "eda", "eda"),
                        PermissionProcessStatus.CREATED
                )
        ));
        var msg = new MockHttpOutputMessage();

        // When & Then
        assertDoesNotThrow(() -> converter.write(csm, MediaType.APPLICATION_XML, msg));
    }

    private static Stream<Arguments> cimAndAgnosticClasses() {
        return Stream.of(
                Arguments.of(ValidatedHistoricalDataMarketDocuments.class),
                Arguments.of(ConnectionStatusMessages.class)
        );
    }
}