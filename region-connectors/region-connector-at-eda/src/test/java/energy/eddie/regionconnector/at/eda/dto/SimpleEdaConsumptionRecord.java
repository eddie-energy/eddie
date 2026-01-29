// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public class SimpleEdaConsumptionRecord implements EdaConsumptionRecord {


    private String conversationId;
    private String meteringPoint;
    private LocalDate startDate;
    private LocalDate endDate;
    private String senderMessageAddress;
    private ZonedDateTime documentCreationDateTime;
    private String receiverMessageAddress;
    private List<Energy> energy;
    private String schemaVersion;
    private XMLGregorianCalendar processDate;
    private Object originalConsumptionRecord;
    private String messageId;

    @Override
    public String messageId() {
        return messageId;
    }

    @Override
    public String conversationId() {
        return conversationId;
    }

    @Override
    public String meteringPoint() {
        return meteringPoint;
    }

    @Override
    public LocalDate startDate() {
        return startDate;
    }

    @Override
    public LocalDate endDate() {
        return endDate;
    }

    @Override
    public String senderMessageAddress() {
        return senderMessageAddress;
    }

    @Override
    public ZonedDateTime documentCreationDateTime() {
        return documentCreationDateTime;
    }

    @Override
    public String receiverMessageAddress() {
        return receiverMessageAddress;
    }

    @Override
    public List<Energy> energy() {
        return energy;
    }

    @Override
    public String schemaVersion() {
        return schemaVersion;
    }

    @Override
    public XMLGregorianCalendar processDate() {
        return processDate;
    }

    @Override
    public Object originalConsumptionRecord() {
        return originalConsumptionRecord;
    }

    public SimpleEdaConsumptionRecord setMessageId(String messageId) {
        this.messageId = messageId;
        return this;
    }

    public SimpleEdaConsumptionRecord setConversationId(String conversationId) {
        this.conversationId = conversationId;
        return this;
    }

    public SimpleEdaConsumptionRecord setMeteringPoint(String meteringPoint) {
        this.meteringPoint = meteringPoint;
        return this;
    }

    public SimpleEdaConsumptionRecord setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public SimpleEdaConsumptionRecord setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }

    public SimpleEdaConsumptionRecord setSenderMessageAddress(String senderMessageAddress) {
        this.senderMessageAddress = senderMessageAddress;
        return this;
    }

    public SimpleEdaConsumptionRecord setDocumentCreationDateTime(ZonedDateTime documentCreationDateTime) {
        this.documentCreationDateTime = documentCreationDateTime;
        return this;
    }

    public SimpleEdaConsumptionRecord setReceiverMessageAddress(String receiverMessageAddress) {
        this.receiverMessageAddress = receiverMessageAddress;
        return this;
    }

    public SimpleEdaConsumptionRecord setEnergy(List<Energy> energy) {
        this.energy = energy;
        return this;
    }

    public SimpleEdaConsumptionRecord setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
        return this;
    }

    public SimpleEdaConsumptionRecord setProcessDate(XMLGregorianCalendar processDate) {
        this.processDate = processDate;
        return this;
    }

    public SimpleEdaConsumptionRecord setOriginalConsumptionRecord(Object originalConsumptionRecord) {
        this.originalConsumptionRecord = originalConsumptionRecord;
        return this;
    }
}
