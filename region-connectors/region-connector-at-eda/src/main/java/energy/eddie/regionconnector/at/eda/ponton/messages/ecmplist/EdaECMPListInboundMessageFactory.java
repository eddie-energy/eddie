// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist;

import energy.eddie.regionconnector.at.eda.dto.EdaECMPList;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;

import java.io.InputStream;

public interface EdaECMPListInboundMessageFactory extends PontonMessageFactory {
    EdaECMPList parseInputStream(InputStream inputStream);
}
