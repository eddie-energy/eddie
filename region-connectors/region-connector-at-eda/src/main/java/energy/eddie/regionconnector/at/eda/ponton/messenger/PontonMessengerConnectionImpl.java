// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import de.ponton.xp.adapter.api.ConnectionException;
import de.ponton.xp.adapter.api.MessengerConnection;
import de.ponton.xp.adapter.api.OutboundMessageStatusUpdateHandler;
import de.ponton.xp.adapter.api.TransmissionException;
import de.ponton.xp.adapter.api.domainvalues.AdapterInfo;
import de.ponton.xp.adapter.api.domainvalues.InboundStatusEnum;
import de.ponton.xp.adapter.api.domainvalues.MessengerInstance;
import de.ponton.xp.adapter.api.messages.InboundMessage;
import de.ponton.xp.adapter.api.messages.InboundMessageStatusUpdate;
import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.models.MessageCodes;
import energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterConfiguration;
import energy.eddie.regionconnector.at.eda.ponton.messages.InboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.ponton.messages.OutboundMessageFactoryCollection;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.XmlMappingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.concurrent.locks.ReentrantLock;

public class PontonMessengerConnectionImpl implements AutoCloseable, PontonMessengerConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(PontonMessengerConnectionImpl.class);
    private final InboundMessageFactoryCollection inboundMessageFactoryCollection;
    private final OutboundMessageFactoryCollection outboundMessageFactoryCollection;
    private final MessengerConnection.MessengerConnectionBuilder messengerConnectionBuilder;
    private final MessengerHealth healthApi;
    private final MessengerMonitor messengerMonitor;
    private final ReentrantLock lock = new ReentrantLock();
    @Nullable
    private OutboundMessageStatusUpdateHandler outboundMessageStatusUpdateHandler;
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
    private MessengerConnection messengerConnection;

    public PontonMessengerConnectionImpl(
            PontonXPAdapterConfiguration config,
            File workFolder,
            InboundMessageFactoryCollection inboundMessageFactoryCollection,
            OutboundMessageFactoryCollection outboundMessageFactoryCollection,
            MessengerHealth healthApi,
            MessengerMonitor messengerMonitor
    ) throws ConnectionException {
        this.inboundMessageFactoryCollection = inboundMessageFactoryCollection;
        this.outboundMessageFactoryCollection = outboundMessageFactoryCollection;
        this.healthApi = healthApi;
        this.messengerMonitor = messengerMonitor;
        final AdapterInfo adapterInfo = createAdapterInfo(config);
        this.messengerConnectionBuilder = createMessengerConnectionBuilder(config, workFolder, adapterInfo);
        this.messengerConnection = this.messengerConnectionBuilder.build();
    }

    @Override
    public void close() {
        messengerConnection.close();
    }

    @Override
    public void start() throws TransmissionException {
        messengerConnection.startReception();
    }

    @Override
    public void sendCMRevoke(CCMORevoke ccmoRevoke) throws TransmissionException, ConnectionException {
        sendMessage(outboundMessageFactoryCollection.activeCmRevokeFactory().createOutboundMessage(ccmoRevoke));
    }

    @Override
    public void sendCMRequest(CCMORequest ccmoRequest) throws TransmissionException, ConnectionException {
        sendMessage(outboundMessageFactoryCollection.activeCmRequestFactory().createOutboundMessage(ccmoRequest));
    }

    @Override
    public void sendCPRequest(CPRequestCR cpRequestCR) throws TransmissionException, ConnectionException {
        sendMessage(outboundMessageFactoryCollection.activeCPRequestFactory().createOutboundMessage(cpRequestCR));
    }

    @Override
    public PontonMessengerConnection withOutboundMessageStatusUpdateHandler(OutboundMessageStatusUpdateHandler outboundMessageStatusUpdateHandler) {
        this.outboundMessageStatusUpdateHandler = outboundMessageStatusUpdateHandler;
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

    public void sendMessage(
            OutboundMessage outboundMessage
    ) throws de.ponton.xp.adapter.api.TransmissionException, ConnectionException {
        try {
            messengerConnection.sendMessage(outboundMessage);
        } catch (Exception sendException) {
            LOGGER.error("Error while sending message to Ponton XP Messenger", sendException);
            // check if the exception is retryable and no other thread is already trying to restart the connection
            if (!(isRetryable(sendException) && lock.tryLock())) {
                throw new ConnectionException("Error while sending message to Ponton XP Messenger", sendException);
            }
            LOGGER.info("Restarting Ponton XP adapter messengerConnection.");
            try {
                messengerConnection.close();
                messengerConnection = messengerConnectionBuilder.build();
                messengerConnection.startReception();
                LOGGER.info("Ponton XP adapter messengerConnection restarted.");
                messengerConnection.sendMessage(outboundMessage);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public MessengerStatus messengerStatus() {
        return healthApi.messengerStatus();
    }

    @Override
    public void resendFailedMessage(ZonedDateTime date, String messageId) {
        messengerMonitor.resendFailedMessage(date, messageId);
    }

    private static AdapterInfo createAdapterInfo(PontonXPAdapterConfiguration config) {
        return AdapterInfo
                .newBuilder()
                .setAdapterId(config.adapterId())
                .setAdapterVersion(config.adapterVersion())
                .build();
    }

    private MessengerConnection.MessengerConnectionBuilder createMessengerConnectionBuilder(
            PontonXPAdapterConfiguration config,
            File workFolder,
            AdapterInfo adapterInfo
    ) {
        return MessengerConnection
                .newBuilder()
                .setWorkFolder(workFolder)
                .setAdapterInfo(adapterInfo)
                .addMessengerInstance(MessengerInstance.create(config.hostname(), config.port()))
                .onAdapterStatusRequest(() -> config.adapterId() + " " + config.adapterVersion() + " is running on.")
                .onMessageStatusUpdate(outboundMessageStatusUpdate -> {
                    if (outboundMessageStatusUpdateHandler != null) {
                        outboundMessageStatusUpdateHandler.onOutboundMessageStatusUpdate(outboundMessageStatusUpdate);
                    }
                })
                .onAdapterStatusRequest(() -> config.adapterId() + " " + config.adapterVersion() + " is running on.")
                .onMessageReceive(this::inboundMessageHandler);
    }

    private InboundMessageStatusUpdate inboundMessageHandler(InboundMessage inboundMessage) {
        var messageType = inboundMessage.getInboundMetaData().getMessageType().getName().getValue();
        var messageVersion = inboundMessage.getInboundMetaData().getMessageType().getVersion().getValue();
        LOGGER.atInfo()
              .addArgument(messageType)
              .addArgument(messageVersion)
              .addArgument(() -> inboundMessage.getInboundMetaData().getConversationId().getValue())
              .log("Received message type: '{}' (version '{}') with ConversationId: '{}'");

        try (InputStream inputStream = inboundMessage.createInputStream()) {
            var result = switch (messageType) {
                case MessageCodes.Notification.ANSWER ->
                        handleNotification(inputStream, NotificationMessageType.CCMO_ANSWER);
                case MessageCodes.Notification.ACCEPT ->
                        handleNotification(inputStream, NotificationMessageType.CCMO_ACCEPT);
                case MessageCodes.Notification.REJECT ->
                        handleNotification(inputStream, NotificationMessageType.CCMO_REJECT);
                case MessageCodes.Revoke.EligibleParty.ANSWER ->
                        handleNotification(inputStream, NotificationMessageType.CCMS_ANSWER);
                case MessageCodes.Revoke.EligibleParty.DENIAL ->
                        handleNotification(inputStream, NotificationMessageType.CCMS_REJECT);
                case MessageCodes.MASTER_DATA -> handleMasterData(inputStream);
                case MessageCodes.CONSUMPTION_RECORD -> handleConsumptionRecord(inputStream);
                case MessageCodes.Revoke.CUSTOMER, MessageCodes.Revoke.IMPLICIT -> handleRevoke(inputStream);
                case MessageCodes.CPNotification.ANSWER ->
                        handleCPNotification(inputStream, CPNotificationMessageType.ANTWORT_PT);
                case MessageCodes.CPNotification.REJECT ->
                        handleCPNotification(inputStream, CPNotificationMessageType.ABLEHNUNG_PT);
                default -> {
                    LOGGER.warn("Received message type '{}' (version '{}') is not supported.",
                                messageType,
                                messageVersion);
                    yield new InboundMessageResult(InboundStatusEnum.REJECTED,
                                                   "message type " + messageType + " version " + messageVersion + " is not supported.");
                }
            };
            return InboundMessageStatusUpdate.newBuilder()
                                             .setInboundMessage(inboundMessage)
                                             .setStatus(result.status())
                                             .setStatusText(result.statusMessage())
                                             .build();
        } catch (XmlMappingException e) {
            LOGGER.atError()
                  .addArgument(messageType)
                  .addArgument(messageVersion)
                  .setCause(e)
                  .log("Error while trying to unmarshal contents of message type '{}' in schema-version '{}'");
            return InboundMessageStatusUpdate.newBuilder()
                                             .setInboundMessage(inboundMessage)
                                             .setStatus(InboundStatusEnum.REJECTED)
                                             .setStatusText(e.getMessage())
                                             .build();
        } catch (IOException e) {
            LOGGER.error("Error while reading input stream of message", e);
            return InboundMessageStatusUpdate.newBuilder()
                                             .setInboundMessage(inboundMessage)
                                             .setStatus(InboundStatusEnum.TEMPORARY_ERROR)
                                             .setStatusText(e.getMessage())
                                             .build();
        }
    }

    private InboundMessageResult handleNotification(
            InputStream inputStream,
            NotificationMessageType notificationMessageType
    ) {
        if (cmNotificationHandler == null) {
            LOGGER.warn("Received notification message, but no handler is available.");
            return new InboundMessageResult(
                    InboundStatusEnum.REJECTED,
                    "No handler available for notification message."
            );
        }

        var notification = inboundMessageFactoryCollection
                .activeCMNotificationFactory()
                .parseInputStream(inputStream);
        return cmNotificationHandler.handle(notification, notificationMessageType);
    }

    private InboundMessageResult handleCPNotification(
            InputStream inputStream,
            CPNotificationMessageType cpNotificationMessageType
    ) {
        if (cpNotificationHandler == null) {
            LOGGER.warn("Received cpnotification message, but no handler is available.");
            return new InboundMessageResult(
                    InboundStatusEnum.REJECTED,
                    "No handler available for cpnotification message."
            );
        }

        var notification = inboundMessageFactoryCollection
                .activeCPNotificationFactory()
                .parseInputStream(inputStream);
        return cpNotificationHandler.handle(notification, cpNotificationMessageType);
    }

    private InboundMessageResult handleMasterData(InputStream inputStream) {
        if (masterDataHandler == null) {
            LOGGER.warn("Received master data message, but no handler is available.");
            return new InboundMessageResult(
                    InboundStatusEnum.REJECTED,
                    "No handler available for master data message."
            );
        }
        EdaMasterData masterData = inboundMessageFactoryCollection
                .activeMasterDataFactory()
                .parseInputStream(inputStream);
        return masterDataHandler.handle(masterData);
    }

    private InboundMessageResult handleConsumptionRecord(InputStream inputStream) {
        if (consumptionRecordHandler == null) {
            LOGGER.warn("Received consumption record message, but no handler is available.");
            return new InboundMessageResult(
                    InboundStatusEnum.REJECTED,
                    "No handler available for consumption record message."
            );
        }
        EdaConsumptionRecord consumptionRecord = inboundMessageFactoryCollection
                .activeConsumptionRecordFactory()
                .parseInputStream(inputStream);
        return consumptionRecordHandler.handle(consumptionRecord);
    }

    private InboundMessageResult handleRevoke(InputStream inputStream) {
        if (cmRevokeHandler == null) {
            LOGGER.warn("Received revoke message, but no handler is available.");
            return new InboundMessageResult(
                    InboundStatusEnum.REJECTED,
                    "No handler available for revoke message."
            );
        }
        EdaCMRevoke cmRevoke = inboundMessageFactoryCollection
                .activeCMRevokeFactory()
                .parseInputStream(inputStream);
        return cmRevokeHandler.handle(cmRevoke);
    }

    private boolean isRetryable(Exception sendException) {
        return sendException instanceof IOException && healthApi.messengerStatus().ok();
    }
}
