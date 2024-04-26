package energy.eddie.regionconnector.shared.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.mint.DefaultJWSMinter;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.shared.security.JwtUtil.JWS_ALGORITHM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {
    private static final String testSecret = "RbNQrp0Dfd+fNoTalQQTd5MRurblhcDtVYaPGoDsg8Q=";
    private JwtUtil jwtUtil;
    @Mock
    private HttpServletRequest mockRequest;
    @Mock
    private HttpServletResponse mockResponse;
    @Mock
    private Cookie mockCookie;

    public static Stream<Arguments> invalidJwtSource() {
        return Stream.of(
                Arguments.of(
                        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NTQ0MjgsInBlcm1pc3Npb25zIjp7ImVzLWRhdGFkaXMiOlsiSGVsbG8gVGVzdCBJRCJdfX0.6SSKolpxyUCFln_cudGxTzDLBJODlHU77v4g5rbKeNI"
                ),
                Arguments.of(
                        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NTQ4MTQsIk5PVF9FWFBFQ1RFRCI6WyJmb28iLCJiYXIiXX0.bFxr9oBmNv-MJr1ewT0TZND7HfmjhfOAEB13LgJYCjw"
                ),
                Arguments.of(
                        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NTUwNTEsInBlcm1pc3Npb25zIjp7fX0.1_08H01oCMpKwuwqMtoOyu2oCKqM7-eyrU_dYdiktW0"
                ),
                Arguments.of(
                        "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NTQ4NzQsInBlcm1pc3Npb25zIjpbImZvbyIsImJhciJdfQ.4adKdit8WWEujzXgqh12PmEIqS2_mSdtpWqnP_DlYJc"
                )
        );
    }

    @BeforeEach
    void setUp() throws JOSEException {
        jwtUtil = new JwtUtil(testSecret);

        byte[] secretBytes = Base64.getDecoder().decode(testSecret);
        ImmutableSecret<SecurityContext> immutableSecret = new ImmutableSecret<>(secretBytes);
        DefaultJWSMinter<SecurityContext> defaultJWSMinter = new DefaultJWSMinter<>();
        defaultJWSMinter.setJWKSource(immutableSecret);

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .build();

        // TODO remove
        HashMap<String, List<String>> value = new HashMap<>();
        value.put("test-rc", List.of("foo", "bar"));
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issueTime(Date.from(Instant.now()))
                .claim("permissions", value)
                .build();
        System.out.println(defaultJWSMinter.mint(header, claimsSet.toPayload(), null).serialize());
    }

    @Test
    void givenInvalidKeyLength_constructor_throws() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("tooShort"));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("tooLoooooooooooooooooooooooooooooooooooong"));
    }

    @Test
    void givenNull_getPermissions_returnsEmptyMap() {
        // When
        var permissions = jwtUtil.getPermissions(null);

        // Then
        assertNotNull(permissions);
        assertEquals(0, permissions.size());
    }

    @ParameterizedTest
    @MethodSource("invalidJwtSource")
    void givenInvalidJwt_getPermissions_returnsEmptyMap(String jwt) {
        // When
        var permissions = jwtUtil.getPermissions(jwt);

        // Then
        assertNotNull(permissions);
        assertEquals(0, permissions.size());
    }

    @Test
    void givenValidJwt_getPermissions_returnsPermissionsInside() {
        // When
        var permissions = jwtUtil.getPermissions(
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NTUxMjEsInBlcm1pc3Npb25zIjp7InNvbWVPdGhlcklkIjpbInNlY29uZEZvbyIsInNlY29uZEJhciJdLCJzb21lSWQiOlsiZm9vIiwiYmFyIl19fQ.iuzmwODIzUbVRYgSDkVyaAb2C7md1uD36gqBGt2vWSA");

        // Then
        assertNotNull(permissions);
        assertEquals(2, permissions.size());
        assertEquals(2, permissions.get("someId").size());
        assertEquals(2, permissions.get("someOtherId").size());
        assertThat(permissions.get("someId")).hasSameElementsAs(List.of("bar", "foo"));
    }

    @Test
    void setJwtCookie_addsExistingAndNewPermissionIds() throws JwtCreationFailedException {
        var keySelector = new JWSVerificationKeySelector<>(JWS_ALGORITHM,
                                                           new ImmutableSecret<>(Base64.getDecoder()
                                                                                       .decode(testSecret)));
        var processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(keySelector);

        // Given
        String existingJwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE3MTI1NjAyMTIsInBlcm1pc3Npb25zIjp7InRlc3QtcmMiOlsiZm9vIiwiYmFyIl19fQ.pb9lkYbzK2JTY9HkRlgb8LBZg35baS_F54kAOE4DD_Y";
        when(mockCookie.getValue()).thenReturn(existingJwt);
        when(mockCookie.getName()).thenReturn(JwtUtil.JWT_COOKIE_NAME);
        when(mockRequest.getCookies()).thenReturn(new Cookie[]{mockCookie});

        // When
        jwtUtil.setJwtCookie(mockRequest, mockResponse, "test-rc", "newPermissionId");

        // Then existing and new permissionId are contained
        verify(mockResponse).setHeader(eq("Set-Cookie"), argThat(setCookieString -> {
            var jwt = setCookieString.substring(JwtUtil.JWT_COOKIE_NAME.length() + 1, setCookieString.indexOf(';'));
            AtomicReference<JWTClaimsSet> claimsSet = new AtomicReference<>();

            assertDoesNotThrow(() -> claimsSet.set(processor.process(jwt, null)));
            assertNotNull(claimsSet.get());
            Map<String, List<String>> permissions = (Map<String, List<String>>) claimsSet.get()
                                                                                         .getClaim(JwtUtil.JWT_PERMISSIONS_CLAIM);

            assertEquals(1, permissions.size());
            assertEquals(3, permissions.get("test-rc").size());
            assertThat(permissions.get("test-rc")).hasSameElementsAs(List.of("foo", "bar", "newPermissionId"));

            assertThat(claimsSet.get().getIssueTime()).isBefore(Instant.now());

            return true;
        }));
    }
}
