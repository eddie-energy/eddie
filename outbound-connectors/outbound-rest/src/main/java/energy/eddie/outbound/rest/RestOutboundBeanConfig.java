package energy.eddie.outbound.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.dto.*;
import energy.eddie.outbound.rest.mixins.ConnectionStatusMessageMixin;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
@EnableConfigurationProperties(RestOutboundConnectorConfiguration.class)
public class RestOutboundBeanConfig {
    @Bean("objectMapper")
    public ObjectMapper objectMapper(ObjectMapper objectMapper) {
        return objectMapper.registerModule(new JavaTimeModule())
                           .registerModule(new Jdk8Module())
                           .registerModule(new JakartaXmlBindAnnotationModule())
                           .addMixIn(ConnectionStatusMessages.class, ConnectionStatusMessageMixin.class);
    }

    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(
                // Agnostic
                ConnectionStatusMessage.class,
                // CIM v0.82
                ValidatedHistoricalDataEnvelope.class,
                PermissionEnvelope.class,
                AccountingPointEnvelope.class,
                // DTOs
                CimCollection.class,
                ValidatedHistoricalDataMarketDocuments.class,
                PermissionMarketDocuments.class,
                AccountingPointDataMarketDocuments.class
        );
        return marshaller;
    }
}
