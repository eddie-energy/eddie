// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services.oauth.revocation;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

public class RevocationSuccessResponse extends RevocationResponse {
    RevocationSuccessResponse(HTTPResponse httpResponse) {super(httpResponse);}

    public static RevocationSuccessResponse parse(HTTPResponse httpResponse) throws ParseException {
        httpResponse.ensureStatusCode(200);
        return new RevocationSuccessResponse(httpResponse);
    }

    @Override
    public boolean indicatesSuccess() {
        return true;
    }
}
