// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.rest.config.RestOutboundConnectorConfiguration;
import energy.eddie.outbound.rest.dto.*;
import energy.eddie.outbound.rest.dto.v1_12.NearRealTimeDataMarketDocuments;
import energy.eddie.outbound.rest.mixins.AgnosticMessageMixin;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.jackson.autoconfigure.XmlMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import java.util.List;

@Configuration
@EnableConfigurationProperties(RestOutboundConnectorConfiguration.class)
public class RestOutboundBeanConfig {
    @Bean
    @Primary
    public JsonMapper jsonMapper(List<JsonMapperBuilderCustomizer> customizers) {
        var builder = JsonMapper.builder();
        customizers.forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    public XmlMapperBuilderCustomizer xmlMapperBuilderCustomizer() {
        return RestOutboundBeanConfig::builderCustomizer;
    }

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return RestOutboundBeanConfig::builderCustomizer;
    }

    private static <M extends ObjectMapper, B extends MapperBuilder<M, B>> void builderCustomizer(MapperBuilder<M, B> builder) {
        builder
                .addModule(new JakartaXmlBindAnnotationModule())
                .addMixIn(ConnectionStatusMessages.class, AgnosticMessageMixin.class)
                .addMixIn(RawDataMessages.class, AgnosticMessageMixin.class);
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
                // CIM v0.91.08
                RTREnvelope.class,
                // CIM v1.04
                VHDEnvelope.class,
                energy.eddie.cim.v1_04.rtd.RTDEnvelope.class,
                // CIM v1.12
                energy.eddie.cim.v1_12.rtd.RTDEnvelope.class,
                // DTOs
                CimCollection.class,
                ValidatedHistoricalDataMarketDocuments.class,
                energy.eddie.outbound.rest.dto.v1_04.NearRealTimeDataMarketDocuments.class,
                NearRealTimeDataMarketDocuments.class,
                PermissionMarketDocuments.class,
                AccountingPointDataMarketDocuments.class,
                ValidatedHistoricalDataMarketDocumentsV1_04.class
        );
        return marshaller;
    }
}
