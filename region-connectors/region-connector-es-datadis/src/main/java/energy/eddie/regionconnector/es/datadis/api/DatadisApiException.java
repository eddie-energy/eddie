// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.api;

import io.netty.handler.codec.http.HttpResponseStatus;

public class DatadisApiException extends Exception {
    private final int statusCode;
    private final String responseBody;
    private final String statusPhrase;
    private final String message;

    public DatadisApiException(String message, HttpResponseStatus status, String responseBody) {
        super("API Exception: " + status.code() + " - " + status.reasonPhrase());
        this.statusCode = status.code();
        this.statusPhrase = status.reasonPhrase();
        this.responseBody = responseBody;
        this.message = message;
    }

    public int statusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return "DatadisApiException{" +
                "message='" + message + '\'' +
                ", statusCode=" + statusCode +
                ", responseBody='" + responseBody + '\'' +
                ", statusPhrase='" + statusPhrase + '\'' +
                '}';
    }
}