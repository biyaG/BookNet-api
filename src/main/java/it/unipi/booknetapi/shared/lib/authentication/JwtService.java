package it.unipi.booknetapi.shared.lib.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import it.unipi.booknetapi.model.user.Role;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    private final Algorithm algorithm;

    public JwtService(KeyUtils keyUtils) {
        // RSA256: Sign with Private, Verify with Public
        this.algorithm = Algorithm.RSA256(keyUtils.getPublicKey(), keyUtils.getPrivateKey());
    }

    public String createToken(UserToken user) {
        return JWT.create()
                .withSubject(user.getUsername())
                .withClaim("name", user.getName())
                .withClaim("role", user.getRole().name()) // Store enum name "ADMIN"
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 2)) // 2 hours validity
                .sign(algorithm);
    }

    public UserToken validateToken(String token) {
        JWTVerifier verifier = JWT.require(algorithm).build();

        DecodedJWT decoded = verifier.verify(token);

        return new UserToken(
                decoded.getClaim("name").asString(),
                decoded.getSubject(), // username
                Role.valueOf(decoded.getClaim("role").asString())
        );
    }

}