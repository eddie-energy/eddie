// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.serde;

import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import energy.eddie.cim.v0_82.pmd.PermissionEnvelope;
import energy.eddie.cim.v0_82.vhd.ValidatedHistoricalDataEnvelope;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;

/**
 * The CIM document types supported by the {@link MessageSerde}, each combined
 * with its CIM version and the concrete class used to deserialize it.
 */
public enum CimMessageTypes implements MessageType {
    // CIM v0.82
    PERMISSION_V0_82(PermissionEnvelope.class),
    VALIDATED_HISTORICAL_DATA_V0_82(ValidatedHistoricalDataEnvelope.class),
    ACCOUNTING_POINT_V0_82(AccountingPointEnvelope.class),
    // CIM v0.91.08
    REDISTRIBUTION_TRANSACTION_REQUEST_V0_91_08(RTREnvelope.class),
    // CIM v1.04
    VALIDATED_HISTORICAL_DATA_V1_04(VHDEnvelope.class),
    REAL_TIME_DATA_V1_04(RTDEnvelope.class),
    // CIM v1.12
    REAL_TIME_DATA_V1_12(energy.eddie.cim.v1_12.rtd.RTDEnvelope.class),
    ACKNOWLEDGEMENT_V1_12(AcknowledgementEnvelope.class),
    ENERGY_SHARING_REFERENCE_DATA_V1_12(ESRDMDEnvelope.class),
    REFERENCE_ENERGY_CURVE_MIN_MAX_OPERATION_V1_12(RECMMOEEnvelope.class),
    PERMISSION_V1_12(RequestPermissionEnvelope.class);

    private final Class<?> messageClass;

    CimMessageTypes(Class<?> messageClass) {
        this.messageClass = messageClass;
    }

    @Override
    public Class<?> messageClass() {
        return messageClass;
    }
}
