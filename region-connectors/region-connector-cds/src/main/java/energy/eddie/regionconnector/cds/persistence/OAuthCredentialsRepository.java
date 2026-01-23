// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.persistence;

import energy.eddie.regionconnector.cds.oauth.OAuthCredentials;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthCredentialsRepository extends CrudRepository<OAuthCredentials, String> {
    OAuthCredentials getOAuthCredentialByPermissionId(String permissionId);

    void deleteByPermissionId(String permissionId);
}
