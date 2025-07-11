package energy.eddie.exampleappbackend.service;

import energy.eddie.exampleappbackend.exception.InvalidUserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class AuthService {
    public UUID getCurrentUserId() throws InvalidUserException {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof Jwt)) {
            log.error("Could not parse UUID from token, because the user is not an Jwt user");
            throw new InvalidUserException();
        }

        var subject = ((Jwt) principal).getSubject();
        try {
            var uuid = UUID.fromString(subject);
            log.debug("Successfully Parsed UUID ({}) from token!", uuid);
            return uuid;
        } catch (IllegalArgumentException ex) {
            log.error("Could not parse UUID from token, because the Jwt User has not valid UUID!");
            throw new InvalidUserException();
        }
    }
}
