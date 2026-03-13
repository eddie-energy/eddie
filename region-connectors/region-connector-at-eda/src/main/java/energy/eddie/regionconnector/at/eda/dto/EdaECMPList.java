// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface EdaECMPList {
    String messageId();

    String conversationId();

    String ecId();

    Optional<ZonedDateTime> endDate(String meterId);

    Object getOriginal();
}
