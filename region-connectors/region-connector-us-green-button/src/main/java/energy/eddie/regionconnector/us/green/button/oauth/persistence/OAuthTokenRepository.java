package energy.eddie.regionconnector.us.green.button.oauth.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthTokenDetails, String> {
}
