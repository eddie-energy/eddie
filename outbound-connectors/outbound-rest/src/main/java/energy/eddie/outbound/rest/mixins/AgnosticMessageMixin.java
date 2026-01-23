// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.mixins;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

public abstract class AgnosticMessageMixin<T> {

    @JsonValue
    public abstract List<T> getMessages();
}
