// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.process.model.validation;

import java.util.List;

public interface Validator<T> {
    List<AttributeError> validate(T value);

}