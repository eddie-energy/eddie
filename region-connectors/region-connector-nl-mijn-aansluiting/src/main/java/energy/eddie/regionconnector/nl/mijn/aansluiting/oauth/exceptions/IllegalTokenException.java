package energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions;

import com.nimbusds.oauth2.sdk.ParseException;

public class IllegalTokenException extends Exception {
    public IllegalTokenException(ParseException cause) {
        super(cause);
    }
}
