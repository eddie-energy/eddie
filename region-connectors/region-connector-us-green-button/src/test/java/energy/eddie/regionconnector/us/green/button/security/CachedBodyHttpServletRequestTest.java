package energy.eddie.regionconnector.us.green.button.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CachedBodyHttpServletRequestTest {

    @Test
    void testGetInputStream_returnsSameInputStreamTwice() throws IOException {
        // Given
        var mock = new MockHttpServletRequest("POST",
                                              "http://localhost:8080/region-connectors/us-green-button/webhook");
        mock.setContent("CONTENT".getBytes(StandardCharsets.UTF_8));
        var request = new CachedBodyHttpServletRequest(mock);

        // When
        var res1 = request.getInputStream();
        var res2 = request.getInputStream();

        // Then
        assertAll(
                () -> assertFalse(res1.isFinished()),
                () -> assertFalse(res2.isFinished()),
                () -> assertArrayEquals(res1.readAllBytes(), res2.readAllBytes())
        );
    }

    @Test
    void testGetReader_returnsSameInputStreamTwice() throws IOException {
        // Given
        var req = new MockHttpServletRequest("POST",
                                             "http://localhost:8080/region-connectors/us-green-button/webhook");
        req.setContent("CONTENT".getBytes(StandardCharsets.UTF_8));
        var request = new CachedBodyHttpServletRequest(req);

        // When
        var res1 = request.getReader();
        var res2 = request.getReader();

        // Then
        assertEquals(res1.readLine(), res2.readLine());
    }

    @Test
    void testCachedBodyServletInputStream_isReady() throws IOException {
        // Given
        var mock = new MockHttpServletRequest("POST",
                                              "http://localhost:8080/region-connectors/us-green-button/webhook");
        mock.setContent("CONTENT".getBytes(StandardCharsets.UTF_8));
        var request = new CachedBodyHttpServletRequest(mock);

        // When
        var res = request.getInputStream();

        // Then
        assertTrue(res.isReady());
    }

    @Test
    void testCachedBodyServletInputStream_finishesAfterReadingAll() throws IOException {
        // Given
        var mock = new MockHttpServletRequest("POST",
                                              "http://localhost:8080/region-connectors/us-green-button/webhook");
        mock.setContent("CONTENT".getBytes(StandardCharsets.UTF_8));
        var request = new CachedBodyHttpServletRequest(mock);
        var is = request.getInputStream();

        // When
        is.readAllBytes();

        // Then
        assertTrue(is.isFinished());
    }
}