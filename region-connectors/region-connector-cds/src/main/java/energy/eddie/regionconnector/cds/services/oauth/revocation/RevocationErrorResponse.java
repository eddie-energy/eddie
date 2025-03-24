package energy.eddie.regionconnector.cds.services.oauth.revocation;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

public class RevocationErrorResponse extends RevocationResponse {
    private final ErrorObject errorObject;

    RevocationErrorResponse(HTTPResponse response, ErrorObject errorObject) {
        super(response);
        this.errorObject = errorObject;
    }

    public static RevocationErrorResponse parse(HTTPResponse response) throws ParseException {
        response.ensureStatusCodeNotOK();
        return new RevocationErrorResponse(response, ErrorObject.parse(response));
    }

    public ErrorObject getErrorObject() {
        return errorObject;
    }
}
