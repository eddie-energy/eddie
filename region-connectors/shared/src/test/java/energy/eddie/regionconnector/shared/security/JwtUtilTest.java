// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.shared.security.JwtUtil.JWS_ALGORITHM;
import static energy.eddie.regionconnector.shared.security.JwtUtil.JWT_PERMISSIONS_CLAIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {
    private static final String TEST_SECRET = "RbNQrp0Dfd+fNoTalQQTd5MRurblhcDtVYaPGoDsg8Q=";
    private JwtUtil jwtUtil;
    private DefaultJWTProcessor<SecurityContext> processor;

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
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET, 24);

        var keySelector = new JWSVerificationKeySelector<>(JWS_ALGORITHM,
                                                           new ImmutableSecret<>(Base64.getDecoder()
                                                                                       .decode(TEST_SECRET)));
        processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(keySelector);
    }

    @Test
    void givenInvalidKeyLength_constructor_throws() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("tooShort", 24));
        assertThrows(IllegalArgumentException.class,
                     () -> new JwtUtil("tooLoooooooooooooooooooooooooooooooooooong", 24));
    }

    @ParameterizedTest
    @ValueSource(ints = {-10, 0})
    void givenInvalidTimeoutDuration_constructor_throws(int timeoutDuration) {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(TEST_SECRET, timeoutDuration));
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

    @SuppressWarnings("unchecked")
    @Test
    void createJwt_returnsJwtWithOnlyNewPermissionId() throws JwtCreationFailedException, BadJOSEException, ParseException, JOSEException {
        // Given
        String[] newPermissionId = new String[]{"myTestId1", "myTestId2"};

        // When
        var jwt = jwtUtil.createJwt("aiida", newPermissionId);

        // Then
        JWTClaimsSet claims = processor.process(jwt, null);
        var permissions = (Map<String, List<String>>) claims.getClaim(JWT_PERMISSIONS_CLAIM);
        assertEquals(1, permissions.size());
        assertArrayEquals(newPermissionId, permissions.get("aiida").toArray(new String[0]));
    }
}
