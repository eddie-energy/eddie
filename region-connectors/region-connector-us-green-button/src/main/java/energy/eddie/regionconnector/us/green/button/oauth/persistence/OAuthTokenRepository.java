// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.oauth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthTokenDetails, String> {

    List<PermissionAuthId> findAllByPermissionIdIn(List<String> permissionIds);

    interface PermissionAuthId {
        String getPermissionId();

        String getAuthUid();
    }
}
