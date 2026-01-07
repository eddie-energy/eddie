package energy.eddie.regionconnector.at.eda.web;

import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.OutboundMessageStatusUpdateHandler;
import de.ponton.xp.adapter.api.TransmissionException;
import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.dto.*;
import energy.eddie.regionconnector.at.eda.ponton.messages.InboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messages.OutboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messenger.*;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.oxm.XmlMappingException;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Map;

@RestController
@RequestMapping("/ponton")
@ConditionalOnProperty(value = "region-connector.at.eda.ponton.messenger.enabled", havingValue = "false")
public class WebPontonConnectionController implements PontonMessengerConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebPontonConnectionController.class);
    private final InboundMessageFactoryCollection inboundMessageFactoryCollection;
    private final OutboundMessageFactoryCollection outboundMessageFactoryCollection;
    private final Sinks.Many<String> cmRequestStream = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<String> ccmoRevokeStream = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<String> cpRequestCrStream = Sinks.many().multicast().onBackpressureBuffer();
    @Nullable
    private CMNotificationHandler cmNotificationHandler;
    @Nullable
    private CMRevokeHandler cmRevokeHandler;
    @Nullable
    private ConsumptionRecordHandler consumptionRecordHandler;
    @Nullable
    private MasterDataHandler masterDataHandler;
    @Nullable
    private CPNotificationHandler cpNotificationHandler;

    public WebPontonConnectionController(
            InboundMessageFactoryCollection inboundMessageFactoryCollection,
            OutboundMessageFactoryCollection outboundMessageFactoryCollection
    ) {
        this.inboundMessageFactoryCollection = inboundMessageFactoryCollection;
        this.outboundMessageFactoryCollection = outboundMessageFactoryCollection;
        LOGGER.warn(
                "Running the Ponton X/P Messenger drop-in replacement REST API. This Feature should only be used for development and test purposes!!!");
    }

    @PostMapping(value = "/cm-notification/{type}", consumes = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            requestBody = @RequestBody(
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerconsent.cmnotification._01p12.CMNotification.class)
                            ),
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerconsent.cmnotification._01p20.CMNotification.class)
                            ),
                    }
            )
    )
    public ResponseEntity<Void> cmNotification(
            HttpServletRequest request,
            @PathVariable NotificationMessageType type
    ) throws IOException {
        if (cmNotificationHandler == null) {
            LOGGER.warn("Received CM Notification, but no handler is available.");
            return ResponseEntity.unprocessableContent().build();
        }
        try {
            EdaCMNotification notification = inboundMessageFactoryCollection
                    .activeCMNotificationFactory()
                    .parseInputStream(request.getInputStream());
            LOGGER.debug("Received CM Notification of type {}", type);
            InboundMessageResult res = cmNotificationHandler.handle(notification, type);
            LOGGER.debug("Published CM Notification with result {} and message {}", res.status(), res.statusMessage());
            return resultToResponseEntity(res);
        } catch (XmlMappingException e) {
            LOGGER.info("Got an XML exception while parsing CM Notification", e);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
    }

    @PostMapping(value = "/cm-revoke", consumes = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            requestBody = @RequestBody(
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerconsent.cmrevoke._01p10.CMRevoke.class)
                            ),
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke.class)
                            ),
                    }
            )
    )
    public ResponseEntity<Void> cmRevoke(HttpServletRequest request) throws IOException {
        if (cmRevokeHandler == null) {
            LOGGER.warn("Received CM Revoke message, but no handler is available.");
            return ResponseEntity.unprocessableContent().build();
        }
        try {
            LOGGER.debug("Received CM Revoke");
            EdaCMRevoke cmRevoke = inboundMessageFactoryCollection
                    .activeCMRevokeFactory()
                    .parseInputStream(request.getInputStream());
            var res = cmRevokeHandler.handle(cmRevoke);
            LOGGER.debug("Published CM Revoke with result {} and message {}", res.status(), res.statusMessage());
            return resultToResponseEntity(res);
        } catch (XmlMappingException e) {
            LOGGER.info("Got an XML exception while parsing CM Revoke", e);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
    }

    @PostMapping(value = "/consumption-record", consumes = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            requestBody = @RequestBody(
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.ConsumptionRecord.class)
                            ),
                    }
            )
    )
    public ResponseEntity<Void> consumptionRecord(HttpServletRequest request) throws IOException {
        if (consumptionRecordHandler == null) {
            LOGGER.warn("Received Consumption Record, but no handler is available.");
            return ResponseEntity.unprocessableContent().build();
        }
        try {
            LOGGER.debug("Received Consumption Record");
            EdaConsumptionRecord consumptionRecord = inboundMessageFactoryCollection
                    .activeConsumptionRecordFactory()
                    .parseInputStream(request.getInputStream());
            var res = consumptionRecordHandler.handle(consumptionRecord);
            LOGGER.debug("Published Consumption Record with result {} and message {}",
                         res.status(),
                         res.statusMessage());
            return resultToResponseEntity(res);
        } catch (XmlMappingException e) {
            LOGGER.info("Got an XML exception while parsing consumption record", e);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
    }

    @PostMapping(value = "/master-data", consumes = MediaType.APPLICATION_XML_VALUE)
    @Operation(
            requestBody = @RequestBody(
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerprocesses.masterdata._01p32.MasterData.class)
                            ),
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerprocesses.masterdata._01p33.MasterData.class)
                            ),
                    }
            )
    )
    public ResponseEntity<Void> masterData(HttpServletRequest request) throws IOException {
        if (masterDataHandler == null) {
            LOGGER.warn("Received MasterData, but no handler is available.");
            return ResponseEntity.unprocessableContent().build();
        }
        try {
            LOGGER.debug("Received MasterData");
            EdaMasterData masterData = inboundMessageFactoryCollection
                    .activeMasterDataFactory()
                    .parseInputStream(request.getInputStream());
            var res = masterDataHandler.handle(masterData);
            LOGGER.debug("Published MasterData with result {} and message {}", res.status(), res.statusMessage());
            return resultToResponseEntity(res);
        } catch (XmlMappingException e) {
            LOGGER.info("Got an XML exception while parsing Master Data", e);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
    }

    @PostMapping(value = "/cp-notification/{type}")
    @Operation(
            requestBody = @RequestBody(
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerprocesses.cpnotification._01p13.CPNotification.class)
                            ),
                    }
            )
    )
    public ResponseEntity<Void> cpNotification(
            HttpServletRequest request,
            @PathVariable CPNotificationMessageType type
    ) throws IOException {
        if (cpNotificationHandler == null) {
            LOGGER.warn("Received CpNotification, but no handler is available.");
            return ResponseEntity.unprocessableContent().build();
        }
        try {
            LOGGER.debug("Received CpNotification");
            EdaCPNotification cpNotification = inboundMessageFactoryCollection
                    .activeCPNotificationFactory()
                    .parseInputStream(request.getInputStream());
            var res = cpNotificationHandler.handle(cpNotification, type);
            LOGGER.debug("Published CpNotification with result {} and message {}", res.status(), res.statusMessage());
            return resultToResponseEntity(res);
        } catch (XmlMappingException e) {
            LOGGER.info("Got an XML exception while parsing CP Notification", e);
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
    }

    @GetMapping(value = "/cm-request", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            responses = @ApiResponse(
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerconsent.cmrequest._01p21.CMRequest.class)
                            ),
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerconsent.cmrequest._01p30.CMRequest.class)
                            ),
                    }
            )
    )
    public ResponseEntity<Flux<String>> cmRequest() {
        return ResponseEntity.ok(cmRequestStream.asFlux());
    }

    @GetMapping(value = "/ccmo-revoke", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            responses = @ApiResponse(
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerconsent.cmrevoke._01p00.CMRevoke.class)
                            ),
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerconsent.cmrevoke._01p10.CMRevoke.class)
                            ),
                    }
            )
    )
    public ResponseEntity<Flux<String>> ccmoRevoke() {
        return ResponseEntity.ok(ccmoRevokeStream.asFlux());
    }

    @GetMapping(value = "/cp-request", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            responses = @ApiResponse(
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = at.ebutilities.schemata.customerprocesses.cprequest._01p12.CPRequest.class)
                            ),
                    }
            )
    )
    public ResponseEntity<Flux<String>> cpRequest() {
        return ResponseEntity.ok(cpRequestCrStream.asFlux());
    }

    @Override
    public MessengerStatus messengerStatus() {
        return new MessengerStatus(Map.of(), true);
    }

    @Override
    public void resendFailedMessage(ZonedDateTime date, String messageId) {
        LOGGER.info("Resend not implemented, message {} will not be resent", messageId);
    }

    @Override
    public void close() {
        LOGGER.info("Closing WebPonton Connection");
    }

    @Override
    public void start() throws TransmissionException {
        LOGGER.info("Starting WebPonton Connection");
    }

    @Override
    public void sendCMRevoke(CCMORevoke ccmoRevoke) throws ConnectionException {
        LOGGER.info("Received CM Revoke");
        OutboundMessage res = outboundMessageFactoryCollection.activeCmRevokeFactory()
                                                              .createOutboundMessage(ccmoRevoke);
        try {
            String text = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            ccmoRevokeStream.tryEmitNext(text);
        } catch (IOException e) {
            throw new ConnectionException("Could not convert input stream to string", e);
        }
    }

    @Override
    public void sendCMRequest(CCMORequest ccmoRequest) throws ConnectionException {
        LOGGER.info("Received CM Request");
        try {
            OutboundMessage res = outboundMessageFactoryCollection.activeCmRequestFactory()
                                                                  .createOutboundMessage(ccmoRequest);
            String text = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            cmRequestStream.tryEmitNext(text);
        } catch (IOException e) {
            throw new ConnectionException("Could not convert input stream to string", e);
        }
    }

    @Override
    public void sendCPRequest(CPRequestCR cpRequestCR) throws ConnectionException {
        LOGGER.info("Received CP Request");
        try {
            OutboundMessage res = outboundMessageFactoryCollection.activeCPRequestFactory()
                                                                  .createOutboundMessage(cpRequestCR);
            String text = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            cpRequestCrStream.tryEmitNext(text);
        } catch (IOException e) {
            throw new ConnectionException("Could not convert input stream to string", e);
        }
    }

    @Override
    public PontonMessengerConnection withOutboundMessageStatusUpdateHandler(OutboundMessageStatusUpdateHandler outboundMessageStatusUpdateHandler) {
        LOGGER.info("OutboundMessageStatusUpdateHandler is not supported");
        return this;
    }

    @Override
    public PontonMessengerConnection withCMNotificationHandler(CMNotificationHandler cmNotificationHandler) {
        this.cmNotificationHandler = cmNotificationHandler;
        return this;
    }

    @Override
    public PontonMessengerConnection withCMRevokeHandler(CMRevokeHandler cmRevokeHandler) {
        this.cmRevokeHandler = cmRevokeHandler;
        return this;
    }

    @Override
    public PontonMessengerConnection withConsumptionRecordHandler(ConsumptionRecordHandler consumptionRecordHandler) {
        this.consumptionRecordHandler = consumptionRecordHandler;
        return this;
    }

    @Override
    public PontonMessengerConnection withMasterDataHandler(MasterDataHandler masterDataHandler) {
        this.masterDataHandler = masterDataHandler;
        return this;
    }

    @Override
    public PontonMessengerConnection withCPNotificationHandler(CPNotificationHandler cpNotificationHandler) {
        this.cpNotificationHandler = cpNotificationHandler;
        return this;
    }

    private static ResponseEntity<Void> resultToResponseEntity(InboundMessageResult res) {
        return switch (res.status()) {
            case SUCCESS -> ResponseEntity.noContent().build();
            case REJECTED -> ResponseEntity.badRequest().build();
            case TEMPORARY_ERROR -> ResponseEntity.internalServerError().build();
        };
    }
}
