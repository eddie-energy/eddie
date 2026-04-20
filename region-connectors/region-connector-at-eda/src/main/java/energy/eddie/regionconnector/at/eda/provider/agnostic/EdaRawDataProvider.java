// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.provider.agnostic;

import energy.eddie.api.agnostic.MessageStream;
import energy.eddie.api.agnostic.RawDataMessageFactory;
import energy.eddie.cim.agnostic.RawDataMessage;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableECMPList;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableMasterData;
import energy.eddie.regionconnector.at.eda.provider.IdentifiableStreams;
import energy.eddie.regionconnector.shared.agnostic.OnRawDataMessagesEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

@Component
@OnRawDataMessagesEnabled
public class EdaRawDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaRawDataProvider.class);
    private final Jaxb2Marshaller marshaller;
    private final Flux<RawDataMessage> rawDataStream;

    @Autowired
    public EdaRawDataProvider(Jaxb2Marshaller marshaller, IdentifiableStreams streams) {
        this(
                marshaller,
                streams.consumptionRecordStream(),
                streams.masterDataStream(),
                streams.ecmpListStream()
        );
    }

    EdaRawDataProvider(
            Jaxb2Marshaller marshaller,
            Flux<IdentifiableConsumptionRecord> identifiableConsumptionRecordFlux,
            Flux<IdentifiableMasterData> identifiableMasterDataFlux,
            Flux<IdentifiableECMPList> ecmpListStream
    ) {
        this.marshaller = marshaller;
        // the mapping method is called for each element for each subscriber if we at some point have multiple subscribers, consider using publish().refCount()
        this.rawDataStream = Flux.merge(
                identifiableConsumptionRecordFlux.flatMap(this::mapToRawDataMessage),
                identifiableMasterDataFlux.flatMap(this::mapToRawDataMessage),
                ecmpListStream.flatMap(this::mapToRawDataMessage)
        );
    }

    @MessageStream(RawDataMessage.class)
    public Flux<RawDataMessage> getRawDataStream() {
        return rawDataStream;
    }

    private Flux<RawDataMessage> mapToRawDataMessage(IdentifiableConsumptionRecord identifiableConsumptionRecord) {
        var writer = new StringWriter();
        try {
            marshaller.marshal(identifiableConsumptionRecord.consumptionRecord()
                                                            .originalConsumptionRecord(),
                               new StreamResult(writer));
        } catch (XmlMappingException e) {
            LOGGER.error("Error while marshalling ConsumptionRecord back into XML for raw data output", e);
            return Flux.empty();
        }
        String rawXml = writer.toString();
        return Flux.fromIterable(identifiableConsumptionRecord.permissionRequests())
                   .map(permissionRequest -> RawDataMessageFactory.create(permissionRequest, rawXml));
    }

    private Mono<RawDataMessage> mapToRawDataMessage(IdentifiableMasterData identifiableMasterData) {
        var writer = new StringWriter();
        try {
            marshaller.marshal(identifiableMasterData.masterData().originalMasterData(), new StreamResult(writer));
        } catch (XmlMappingException e) {
            LOGGER.error("Error while marshalling MasterData back into XML for raw data output", e);
            return Mono.empty();
        }
        String rawXml = writer.toString();
        var permissionRequest = identifiableMasterData.permissionRequest();
        var msg = RawDataMessageFactory.create(permissionRequest, rawXml);
        return Mono.just(msg);
    }

    private Mono<RawDataMessage> mapToRawDataMessage(IdentifiableECMPList ecmpList) {
        var writer = new StringWriter();
        try {
            marshaller.marshal(ecmpList.ecmpList().getOriginal(), new StreamResult(writer));
        } catch (XmlMappingException e) {
            LOGGER.error("Error while marshalling MasterData back into XML for raw data output", e);
            return Mono.empty();
        }
        String rawXml = writer.toString();
        var msg = RawDataMessageFactory.create(ecmpList.permissionRequest(), rawXml);
        return Mono.just(msg);
    }
}
