// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages;

import java.time.LocalDate;

public interface PontonMessageFactory {
    boolean isActive(LocalDate date);
}
