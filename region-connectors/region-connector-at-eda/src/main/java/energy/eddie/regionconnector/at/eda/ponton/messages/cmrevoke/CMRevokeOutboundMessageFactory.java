// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke;

import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;

public interface CMRevokeOutboundMessageFactory extends PontonMessageFactory {
    OutboundMessage createOutboundMessage(CCMORevoke ccmoRevoke);
}
