// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.web.cim.v1_12;

import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.cim.v1_12.vhd.MessageDocumentHeader;
import energy.eddie.cim.v1_12.vhd.MetaInformation;
import energy.eddie.outbound.rest.connectors.cim.v1_12.CimConnector;
import energy.eddie.outbound.rest.model.cim.v1_12.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.cim.v1_12.*;
import energy.eddie.outbound.rest.web.EventStream;
import energy.eddie.outbound.rest.web.FallbackXmlMessageConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.dataformat.xml.XmlMapper;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HistoricalDataStoredXmlTest {
    @Mock
    private ValidatedHistoricalDataMarketDocumentRepository repository;
    @Mock
    private NearRealTimeDataMarketDocumentRepository rtdRepository;
    @Mock
    private AcknowledgementMarketDocumentRepository ackRepository;
    @Mock
    private EnergySharingReferenceDataMarketDocumentRepository esrRepository;
    @Mock
    private MinMaxEnvelopeMarketDocumentRepository minMaxRepository;
    @Mock
    private RequestPermissionMarketDocumentRepository rpmdRepository;

    @Test
    void storedXmlUsesRegisteredCimCollectionAndExactVersionedEndpoint() throws Exception {
        var envelope = new Envelope().withMessageDocumentHeader(new MessageDocumentHeader().withMetaInformation(
                new MetaInformation().withRequestPermissionId("pid").withConnectionId("cid").withDataNeedId("dnid")));
        when(repository.findAll(ArgumentMatchers.<PredicateSpecification<ValidatedHistoricalDataMarketDocumentModel>>any()))
                .thenReturn(List.of(new ValidatedHistoricalDataMarketDocumentModel(envelope)));
        var controller = new CimController(
                new CimConnector(), rtdRepository, ackRepository, esrRepository, minMaxRepository, rpmdRepository,
                repository, new EventStream()
        );
        var converter = new FallbackXmlMessageConverter(new JacksonXmlHttpMessageConverter(XmlMapper.builder().build()));
        var client = MockMvcBuilders.standaloneSetup(controller).setMessageConverters(converter).build();
        client.perform(get("/cim_1_12/validated-historical-data-md").param("permissionId", "pid").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("ValidatedHistoricalDataMarketDocuments")))
                .andExpect(content().string(containsString("CEEDS_ValidatedHistoricalDataDocument_v1.12.38")))
                .andExpect(content().string(containsString("requestPermissionId>pid")));
        verify(repository).findAll(ArgumentMatchers.<PredicateSpecification<ValidatedHistoricalDataMarketDocumentModel>>any());
    }
}
