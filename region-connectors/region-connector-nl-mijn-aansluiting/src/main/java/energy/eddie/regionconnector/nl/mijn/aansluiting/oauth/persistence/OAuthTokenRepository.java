package energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthTokenDetails, String> {
}
