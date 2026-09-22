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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CimMessageTypesTest {

    @Test
    void testMessageClass_returnsExpectedClass() {
        assertEquals(PermissionEnvelope.class, CimMessageTypes.PERMISSION_V0_82.messageClass());
        assertEquals(ValidatedHistoricalDataEnvelope.class,
                     CimMessageTypes.VALIDATED_HISTORICAL_DATA_V0_82.messageClass());
        assertEquals(AccountingPointEnvelope.class, CimMessageTypes.ACCOUNTING_POINT_V0_82.messageClass());
        assertEquals(RTREnvelope.class, CimMessageTypes.REDISTRIBUTION_TRANSACTION_REQUEST_V0_91_08.messageClass());
        assertEquals(VHDEnvelope.class, CimMessageTypes.VALIDATED_HISTORICAL_DATA_V1_04.messageClass());
        assertEquals(RTDEnvelope.class, CimMessageTypes.REAL_TIME_DATA_V1_04.messageClass());
        assertEquals(energy.eddie.cim.v1_12.rtd.RTDEnvelope.class, CimMessageTypes.REAL_TIME_DATA_V1_12.messageClass());
        assertEquals(AcknowledgementEnvelope.class, CimMessageTypes.ACKNOWLEDGEMENT_V1_12.messageClass());
        assertEquals(ESRDMDEnvelope.class, CimMessageTypes.ENERGY_SHARING_REFERENCE_DATA_V1_12.messageClass());
        assertEquals(RECMMOEEnvelope.class,
                     CimMessageTypes.REFERENCE_ENERGY_CURVE_MIN_MAX_OPERATION_V1_12.messageClass());
        assertEquals(RequestPermissionEnvelope.class, CimMessageTypes.PERMISSION_V1_12.messageClass());
    }
}
