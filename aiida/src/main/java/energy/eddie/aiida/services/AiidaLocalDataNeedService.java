// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.repositories.AiidaLocalDataNeedRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AiidaLocalDataNeedService {
    private final AiidaLocalDataNeedRepository repository;

    public AiidaLocalDataNeedService(AiidaLocalDataNeedRepository repository) {
        this.repository = repository;
    }

    public Optional<AiidaLocalDataNeed> optionalAiidaLocalDataNeedById(UUID id) {
        return repository.findById(id);
    }
}
