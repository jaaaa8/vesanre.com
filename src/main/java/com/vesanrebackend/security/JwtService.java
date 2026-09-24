package com.vesanrebackend.security;

import com.vesanrebackend.dto.auth.LoginResponse;
import com.vesanrebackend.dto.auth.UserProfileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final long expiresIn;

    public JwtService(JwtEncoder encoder, Clock clock, @Value("${app.security.jwt.issuer}") String issuer,
                      @Value("${app.security.jwt.access-token-ttl-seconds}") long expiresIn) {
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.expiresIn = expiresIn;
    }

    public LoginResponse issue(UserProfileResponse user) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(expiresIn))
                .subject(user.id().toString())
                .claim("email", user.email())
                .claim("roles", user.roles())
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims))
                .getTokenValue();
        return new LoginResponse(token, "Bearer", expiresIn, user);
    }
}
