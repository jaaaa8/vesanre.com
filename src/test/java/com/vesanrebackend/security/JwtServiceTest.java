package com.vesanrebackend.security;

import com.vesanrebackend.dto.auth.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {
    @Test
    void issuesShortLivedTokenWithSubjectAndRoles() {
        Instant now = Instant.parse("2026-09-21T00:00:00Z");
        JwtEncoder encoder = mock(JwtEncoder.class);
        when(encoder.encode(any())).thenReturn(new Jwt("signed-token", now, now.plusSeconds(900),
                Map.of("alg", "HS256"), Map.of("sub", "placeholder")));
        JwtService service = new JwtService(encoder, Clock.fixed(now, ZoneOffset.UTC), "sporthub", 900);
        UUID id = UUID.randomUUID();
        UserProfileResponse user = new UserProfileResponse(id, "member@example.com", "Member", null,
                Set.of("CUSTOMER"), null);

        var response = service.issue(user);
        JwtEncoderParameters parameters = org.mockito.Mockito.mockingDetails(encoder).getInvocations().stream()
                .map(invocation -> (JwtEncoderParameters) invocation.getArgument(0))
                .findFirst().orElseThrow();

        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(parameters.getClaims().getSubject()).isEqualTo(id.toString());
        assertThat(parameters.getClaims().getExpiresAt()).isEqualTo(now.plusSeconds(900));
        assertThat(parameters.getClaims().getClaimAsStringList("roles")).containsExactly("CUSTOMER");
    }
}
