// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds;

import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.cds.client.CdsServerClient;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.dtos.CdsServerMasterData;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CdsRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "cds";
    private final CdsServerClientFactory factory;

    public CdsRegionConnectorMetadata(CdsServerClientFactory factory) {
        this.factory = factory;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public List<String> countryCodes() {
        var countries = factory.getAll()
                               .flatMap(CdsServerClient::masterData)
                               .flatMapIterable(CdsServerMasterData::countries)
                               .collect(Collectors.toSet())
                               .block();
        if (countries == null) {
            return List.of();
        }
        return countries.stream().toList();
    }

    @Override
    public String countryCode() {
        return String.join("/", countryCodes());
    }

    @Override
    public long coveredMeteringPoints() {
        return 0;
    }

    @Override
    public Period earliestStart() {
        return Period.ofYears(-2);
    }

    @Override
    public Period latestEnd() {
        return Period.ofYears(3);
    }

    @Override
    public ZoneId timeZone() {
        return ZoneOffset.UTC;
    }
}
