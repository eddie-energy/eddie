// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.permission;

import energy.eddie.api.agnostic.aiida.AiidaAsset;

import java.util.Set;

public class AssetNotAllowedException extends Exception {
    public AssetNotAllowedException(AiidaAsset asset, Set<AiidaAsset> allowedAssets) {
        super("The asset '%s' is not allowed by the data need, which requires one of %s.".formatted(asset,
                                                                                                  allowedAssets));
    }
}
