package energy.eddie.regionconnector.cds.services.oauth.revocation;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Response;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

public abstract class RevocationResponse implements Response {
    protected final HTTPResponse httpResponse;

    protected RevocationResponse(HTTPResponse httpResponse) {this.httpResponse = httpResponse;}

    public static RevocationResponse parse(HTTPResponse httpResponse) throws ParseException {
        if (httpResponse.getStatusCode() == HTTPResponse.SC_OK) {
            return RevocationSuccessResponse.parse(httpResponse);
        } else {
            return RevocationErrorResponse.parse(httpResponse);
        }
    }

    @Override
    public boolean indicatesSuccess() {
        return this instanceof RevocationSuccessResponse;
    }

    @Override
    public HTTPResponse toHTTPResponse() {
        return httpResponse;
    }

    public RevocationErrorResponse toErrorResponse() {
        return (RevocationErrorResponse) this;
    }
}
