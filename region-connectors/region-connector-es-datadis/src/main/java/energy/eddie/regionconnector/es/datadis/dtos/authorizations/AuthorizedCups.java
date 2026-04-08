// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.dtos.authorizations;

import org.jspecify.annotations.Nullable;

import java.time.ZonedDateTime;

public record AuthorizedCups(Long id,
                             String ownerDocument,
                             String ownerName,
                             String ownerDocumentTypeName,
                             String requesterDocument,
                             String requesterDocumentTypeName,
                             String requesterName,
                             Long requestId,
                             String cups,
                             AuthorizationStatus status,
                             ZonedDateTime validityDateStart,
                             ZonedDateTime validityDateEnd,
                             @Nullable Object isDeleted,
                             String distributorCodeFather) {

    public boolean isCompany() {
        return ownerDocumentTypeName.equals("CIF");
    }

    @Nullable
    public String companyName() {
        return isCompany() ? ownerName : null;
    }

    @Nullable
    public String firstname() {
        if (isCompany()) {
            return null;
        }
        if (!ownerName.contains(" ")) {
            return null;
        }
        return ownerName.substring(0, ownerName.lastIndexOf(" "));
    }

    @Nullable
    public String surname() {
        if (isCompany()) {
            return null;
        }
        return ownerName.substring(ownerName.lastIndexOf(" ") + 1);
    }
}
