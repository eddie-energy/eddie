package energy.eddie.regionconnector.at.eda.web;

import de.ponton.xp.adapter.api.domainvalues.InboundStatusEnum;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.InboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messages.MarshallerConfig;
import energy.eddie.regionconnector.at.eda.ponton.messages.OutboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification._01p12.EdaCMNotification01p12InboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest._01p21.CMRequest01p21OutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p00.CMRevoke01p00OutboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke._01p00.EdaCMRevoke01p00InboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p41.EdaConsumptionRecord01p41InboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cpnotification._1p13.EdaCPNotification01p13InboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.cprequest._1p12.CPRequestOutbound01p12MessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32.EdaMasterData01p32InboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messenger.CPNotificationMessageType;
import energy.eddie.regionconnector.at.eda.ponton.messenger.InboundMessageResult;
import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import energy.eddie.regionconnector.at.eda.requests.*;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = WebPontonConnectionController.class, properties = "region-connector.at.eda.ponton.messenger.enabled=false")
@Import({MarshallerConfig.class, WebPontonConnectionControllerTest.TestConfig.class})
@AutoConfigureMockMvc(addFilters = false)   // disables spring security filters
class WebPontonConnectionControllerTest {
    private final ClassLoader classLoader = getClass().getClassLoader();
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private WebPontonConnectionController controller;

    @BeforeEach
    void setup() {
        controller.withCMNotificationHandler(null)
                  .withCMRevokeHandler(null)
                  .withConsumptionRecordHandler(null)
                  .withCPNotificationHandler(null)
                  .withMasterDataHandler(null)
                  .withOutboundMessageStatusUpdateHandler(null);
    }

    @Test
    void givenCmNotification_whenPostToEndpoint_thenReturnNoContent() throws Exception {
        // Given
        InputStream xml = classLoader.getResourceAsStream("xsd/cmnotification/_01p12/accepted_ccmo.xml");
        assert xml != null;
        controller.withCMNotificationHandler((cmNotification, notificationMessageType) -> {
            assertAll(
                    () -> assertEquals(NotificationMessageType.CCMO_ACCEPT, notificationMessageType),
                    () -> assertNotNull(cmNotification)
            );
            return new InboundMessageResult(InboundStatusEnum.SUCCESS, "");
        });

        // When
        mockMvc.perform(post("/ponton/cm-notification/CCMO_ACCEPT")
                                .contentType(MediaType.APPLICATION_XML)
                                .content(xml.readAllBytes()))
               // Then
               .andExpect(status().isNoContent());

        // Clean Up
        xml.close();
    }

    @Test
    void givenCmRevoke_whenPostToEndpoint_thenReturnNoContent() throws Exception {
        // Given
        InputStream xml = classLoader.getResourceAsStream("xsd/cmrevoke/_01p00/cmrevoke.xml");
        assert xml != null;
        controller.withCMRevokeHandler(message -> {
            assertNotNull(message);
            return new InboundMessageResult(InboundStatusEnum.SUCCESS, "");
        });

        // When
        mockMvc.perform(post("/ponton/cm-revoke")
                                .contentType(MediaType.APPLICATION_XML)
                                .content(xml.readAllBytes()))
               // Then
               .andExpect(status().isNoContent());

        // Clean Up
        xml.close();
    }

    @Test
    void givenConsumptionRecord_whenPostToEndpoint_thenReturnNoContent() throws Exception {
        // Given
        InputStream xml = classLoader.getResourceAsStream(
                "xsd/consumptionrecord/_01p41/consumptionrecord_quater-hourly.xml"
        );
        assert xml != null;
        controller.withConsumptionRecordHandler(message -> {
            assertNotNull(message);
            return new InboundMessageResult(InboundStatusEnum.SUCCESS, "");
        });

        // When
        mockMvc.perform(post("/ponton/consumption-record")
                                .contentType(MediaType.APPLICATION_XML)
                                .content(xml.readAllBytes()))
               // Then
               .andExpect(status().isNoContent());

        // Clean Up
        xml.close();
    }

    @Test
    void givenMasterData_whenPostToEndpoint_thenReturnNoContent() throws Exception {
        // Given
        InputStream xml = classLoader.getResourceAsStream("xsd/masterdata/_01p32/masterdata.xml");
        assert xml != null;
        controller.withMasterDataHandler(message -> {
            assertNotNull(message);
            return new InboundMessageResult(InboundStatusEnum.SUCCESS, "");
        });

        // When
        mockMvc.perform(post("/ponton/master-data")
                                .contentType(MediaType.APPLICATION_XML)
                                .content(xml.readAllBytes()))
               // Then
               .andExpect(status().isNoContent());

        // Clean Up
        xml.close();
    }

    @Test
    void givenCpNotification_whenPostToEndpoint_thenReturnNoContent() throws Exception {
        // Given
        InputStream xml = classLoader.getResourceAsStream("xsd/cpnotification/_01p12/answer_pt.xml");
        assert xml != null;
        controller.withCPNotificationHandler((message, type) -> {
            assertAll(
                    () -> assertEquals(CPNotificationMessageType.ANTWORT_PT, type),
                    () -> assertNotNull(message)
            );
            return new InboundMessageResult(InboundStatusEnum.SUCCESS, "");
        });

        // When
        mockMvc.perform(post("/ponton/cp-notification/ANTWORT_PT")
                                .contentType(MediaType.APPLICATION_XML)
                                .content(xml.readAllBytes()))
               // Then
               .andExpect(status().isNoContent());

        // Clean Up
        xml.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/ponton/cm-notification/CCMO_ACCEPT",
            "/ponton/cm-revoke",
            "/ponton/consumption-record",
            "/ponton/master-data",
            "/ponton/cp-notification/ANTWORT_PT"
    })
    void givenMessageWithoutHandler_whenPostToEndpoint_thenReturnUnprocessableEntity(String endpoint) throws Exception {
        // Given

        // When
        mockMvc.perform(post(endpoint)
                                .contentType(MediaType.APPLICATION_XML)
                                .content("<xml></xml>"))
               // Then
               .andExpect(status().isUnprocessableContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/ponton/cm-notification/CCMO_ACCEPT",
            "/ponton/cm-revoke",
            "/ponton/consumption-record",
            "/ponton/master-data",
            "/ponton/cp-notification/ANTWORT_PT"
    })
    void givenInvalidMessageFormat_whenPostToEndpoint_thenReturnUnsupportedMediaType(String endpoint) throws Exception {
        // Given
        controller.withCMNotificationHandler(this::inboundMessageResultBiFunction);
        controller.withCMRevokeHandler(this::inboundMessageResultFunction);
        controller.withCPNotificationHandler(this::inboundMessageResultBiFunction);
        controller.withMasterDataHandler(this::inboundMessageResultFunction);
        controller.withOutboundMessageStatusUpdateHandler(this::inboundMessageResultFunction);
        controller.withConsumptionRecordHandler(this::inboundMessageResultFunction);
        var invalidXml = "INVALID XML";

        // When
        mockMvc.perform(post(endpoint)
                                .contentType(MediaType.APPLICATION_XML)
                                .content(invalidXml))
               // Then
               .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void givenCcmoRevoke_whenSendCmRevoke_thenDoesNotThrow() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var ccmoRevoke = new CCMORevoke(
                new SimplePermissionRequest(
                        "pid",
                        "cid",
                        "did",
                        "cmRequestId",
                        "convId",
                        "dsoId",
                        Optional.empty(),
                        now,
                        now,
                        PermissionProcessStatus.ACCEPTED,
                        Optional.empty()
                ),
                "EP100",
                "Reason"
        );

        // When & Then
        assertDoesNotThrow(() -> controller.sendCMRevoke(ccmoRevoke));
    }


    @Test
    void givenCcmoRequest_whenSendCmRequest_thenDoesNotThrow() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var ccmoRequest = new CCMORequest(
                new DsoIdAndMeteringPoint("AT100", null),
                new CCMOTimeFrame(now, now),
                "cmRequestId",
                "msg",
                RequestDataType.METERING_DATA,
                AllowedGranularity.P1D,
                AllowedTransmissionCycle.D,
                new AtConfiguration("EP100"),
                now.atStartOfDay(ZoneOffset.UTC)
        );

        // When & Then
        assertDoesNotThrow(() -> controller.sendCMRequest(ccmoRequest));
    }

    @Test
    void givenCpRequest_whenSendCpRequest_thenDoesNotThrow() {
        // Given
        var now = LocalDate.now(ZoneOffset.UTC);
        var request = new CPRequestCR(
                "At100",
                "mid",
                "messageId",
                now,
                now,
                null,
                new AtConfiguration("EP100")
        );

        // When & Then
        assertDoesNotThrow(() -> controller.sendCPRequest(request));
    }

    private InboundMessageResult inboundMessageResultBiFunction(Object obj1, Object obj2) {
        return new InboundMessageResult(InboundStatusEnum.SUCCESS, "");
    }

    private InboundMessageResult inboundMessageResultFunction(Object obj) {
        return new InboundMessageResult(InboundStatusEnum.SUCCESS, "");
    }

    @TestConfiguration
    public static class TestConfig {
        @Bean
        InboundMessageFactoryCollection inboundMessageFactoryCollection(Jaxb2Marshaller jaxb2Marshaller) {
            return new InboundMessageFactoryCollection(
                    List.of(new EdaConsumptionRecord01p41InboundMessageFactory(jaxb2Marshaller)),
                    List.of(new EdaMasterData01p32InboundMessageFactory(jaxb2Marshaller)),
                    List.of(new EdaCMNotification01p12InboundMessageFactory(jaxb2Marshaller)),
                    List.of(new EdaCMRevoke01p00InboundMessageFactory(jaxb2Marshaller)),
                    List.of(new EdaCPNotification01p13InboundMessageFactory(jaxb2Marshaller))
            );
        }

        @Bean
        OutboundMessageFactoryCollection outboundMessageFactoryCollection(Jaxb2Marshaller jaxb2Marshaller) {
            return new OutboundMessageFactoryCollection(
                    List.of(new CMRequest01p21OutboundMessageFactory(jaxb2Marshaller)),
                    List.of(new CMRevoke01p00OutboundMessageFactory(jaxb2Marshaller)),
                    List.of(new CPRequestOutbound01p12MessageFactory(jaxb2Marshaller))
            );
        }
    }
}