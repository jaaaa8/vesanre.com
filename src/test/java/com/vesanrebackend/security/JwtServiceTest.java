package com.vesanrebackend.security;

import com.vesanrebackend.dto.auth.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {
    private final SecurityConfig config = new SecurityConfig();
    private final SecretKey key = config.jwtSecretKey("test-secret-at-least-thirty-two-bytes-long");

    @Test
    void issuedTokenIsAcceptedByDecoderWithSubjectRolesAndTtl() {
        JwtService service = new JwtService(config.jwtEncoder(key), Clock.systemUTC(), "sporthub", 900);
        JwtDecoder decoder = config.jwtDecoder(key, "sporthub");
        UUID id = UUID.randomUUID();

        var response = service.issue(new UserProfileResponse(id, "member@example.com", "Member", null,
                Set.of("CUSTOMER"), null));
        Jwt jwt = decoder.decode(response.accessToken());

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(jwt.getSubject()).isEqualTo(id.toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("sporthub");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CUSTOMER");
        assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plusSeconds(900));
    }
}
