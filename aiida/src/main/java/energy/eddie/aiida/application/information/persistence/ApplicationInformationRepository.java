// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.application.information.persistence;

import energy.eddie.aiida.application.information.ApplicationInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationInformationRepository extends JpaRepository<ApplicationInformation, UUID> {
    Optional<ApplicationInformation> findFirstByOrderByCreatedAtDesc();
}
