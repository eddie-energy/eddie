// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata;

import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;

import java.io.InputStream;

public interface EdaMasterDataInboundMessageFactory extends PontonMessageFactory {
    EdaMasterData parseInputStream(InputStream inputStream);
}
