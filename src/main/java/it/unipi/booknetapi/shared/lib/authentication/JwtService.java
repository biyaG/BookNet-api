package it.unipi.booknetapi.shared.lib.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.model.user.User;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    private static final Long EXPIRATION_TIME = (long) 1000 * 60 * 60 * 2; // in milliseconds

    private final Algorithm algorithm;

    public JwtService(KeyUtils keyUtils) {
        // RSA256: Sign with Private, Verify with Public
        this.algorithm = Algorithm.RSA256(keyUtils.getPublicKey(), keyUtils.getPrivateKey());
    }

    public String createToken(User user) {
        return createToken(new UserToken(user));
    }

    public String createToken(UserToken user) {
        return JWT.create()
                .withSubject(user.getIdUser())
                .withClaim("username", user.getUsername())
                .withClaim("name", user.getName())
                .withClaim("role", user.getRole().name()) // Store enum name "ADMIN"
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 2 hours validity
                .sign(algorithm);
    }

    public String refreshToken(String token) {
        UserToken userToken = validateToken(token.replace("Bearer ", ""));
        return createToken(userToken);
    }

    private Date extractExpiration(String token) {
        return JWT.decode(token).getExpiresAt();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public UserToken validateToken(String token) {
        JWTVerifier verifier = JWT.require(algorithm).build();

        DecodedJWT decoded = verifier.verify(token);

        return new UserToken(
                decoded.getSubject(), // idUser
                decoded.getClaim("name").asString(),
                decoded.getClaim("username").asString(),
                Role.valueOf(decoded.getClaim("role").asString())
        );
    }

}