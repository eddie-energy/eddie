package energy.eddie.regionconnector.de.eta.oauth;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeEtaOAuthTokenRepository extends CrudRepository<DeEtaOAuthToken, Long> {
    Optional<DeEtaOAuthToken> findByConnectionId(String connectionId);
}
