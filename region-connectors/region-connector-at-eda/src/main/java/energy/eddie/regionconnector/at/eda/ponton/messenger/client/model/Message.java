package energy.eddie.regionconnector.at.eda.ponton.messenger.client.model;

import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public record Message(long id,
                      boolean inbound,
                      Status status,
                      boolean test,
                      ZonedDateTime creationTime,
                      ZonedDateTime registrationTime,
                      String senderId,
                      String receiverId,
                      String messageId,
                      String conversationId,
                      @Nullable Integer sequenceNumber,
                      String schemaSet,
                      String messageType,
                      String transmissionProtocol,
                      String packager,
                      String adapterId,
                      boolean ackReceived,
                      String logInfo,
                      Integer clusterNodeId) {}
