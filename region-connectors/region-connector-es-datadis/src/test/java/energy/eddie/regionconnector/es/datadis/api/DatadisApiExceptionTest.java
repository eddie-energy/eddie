// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.api;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatadisApiExceptionTest {

    @Test
    void statusCode_returnsGivenCode() {
        var exception = new DatadisApiException("message", HttpResponseStatus.BAD_REQUEST, "responseBody");
        assertEquals(HttpResponseStatus.BAD_REQUEST.code(), exception.statusCode());
    }

    @Test
    void getMessage() {
        var exception = new DatadisApiException("message", HttpResponseStatus.BAD_REQUEST, "responseBody");
        assertEquals("DatadisApiException{message='message', statusCode=400, responseBody='responseBody', statusPhrase='Bad Request'}", exception.getMessage());
    }
}