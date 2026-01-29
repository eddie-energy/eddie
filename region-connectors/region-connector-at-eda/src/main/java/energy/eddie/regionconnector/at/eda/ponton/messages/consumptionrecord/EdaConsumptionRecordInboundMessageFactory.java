// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord;

import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;

import java.io.InputStream;

public interface EdaConsumptionRecordInboundMessageFactory extends PontonMessageFactory {
    EdaConsumptionRecord parseInputStream(InputStream inputStream);
}
