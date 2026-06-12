// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest;

import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.outbound.rest.dto.AccountingPointDataMarketDocuments;
import energy.eddie.outbound.rest.dto.PermissionMarketDocuments;
import energy.eddie.outbound.rest.dto.ValidatedHistoricalDataMarketDocuments;
import energy.eddie.outbound.rest.dto.ValidatedHistoricalDataMarketDocumentsV1_04;
import energy.eddie.outbound.rest.dto.v1_12.AcknowledgementMarketDocuments;
import energy.eddie.outbound.rest.dto.v1_12.EnergySharingReferenceDataMarketDocuments;
import energy.eddie.outbound.rest.dto.v1_12.NearRealTimeDataMarketDocuments;
import energy.eddie.outbound.rest.dto.v1_12.RequestPermissionMarketDocuments;

import java.util.List;

public class RestCimConfig {
    private RestCimConfig() {
        throw new IllegalStateException("Utility class");
    }

    public static List<List<Class<?>>> cimClasses() {
        return List.of(
                // CIM v0.82
                List.of(ValidatedHistoricalDataEnvelope.class, ValidatedHistoricalDataMarketDocuments.class),
                List.of(PermissionEnvelope.class, PermissionMarketDocuments.class),
                List.of(AccountingPointEnvelope.class, AccountingPointDataMarketDocuments.class),
                // CIM v0.91.08
                List.of(RTREnvelope.class),
                // CIM v1.04
                List.of(VHDEnvelope.class, ValidatedHistoricalDataMarketDocumentsV1_04.class),
                List.of(energy.eddie.cim.v1_04.rtd.RTDEnvelope.class,
                        energy.eddie.outbound.rest.dto.v1_04.NearRealTimeDataMarketDocuments.class),
                // CIM v1.12
                List.of(energy.eddie.cim.v1_12.rtd.RTDEnvelope.class, NearRealTimeDataMarketDocuments.class),
                List.of(energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope.class, AcknowledgementMarketDocuments.class),
                List.of(energy.eddie.cim.v1_12.esr.ESRDMDEnvelope.class,
                        EnergySharingReferenceDataMarketDocuments.class),
                List.of(energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope.class,
                        RequestPermissionMarketDocuments.class)
        );
    }
}
