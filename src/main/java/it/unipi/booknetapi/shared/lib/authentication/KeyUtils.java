package it.unipi.booknetapi.shared.lib.authentication;

import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
// import java.security.KeyPair;
// import java.security.KeyPairGenerator;
import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@Getter
public class KeyUtils {

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public KeyUtils(AppConfig appConfig) {
        try {
            // Generate 2048-bit RSA Key Pair
            /*
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
            this.publicKey = (RSAPublicKey) pair.getPublic();
            */

            this.privateKey = loadPrivateKey(appConfig.getPrivateKeyPath());
            this.publicKey = loadPublicKey(appConfig.getPublicKeyPath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA keys", e);
        }
    }

    // --- Helper: Load Private Key (PKCS#8) ---
    private RSAPrivateKey loadPrivateKey(String path) throws Exception {
        String keyContent = readFile(path);

        // Remove headers/footers and newlines
        String privateKeyPEM = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", ""); // Remove newlines/spaces

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    // --- Helper: Load Public Key (X.509) ---
    private RSAPublicKey loadPublicKey(String path) throws Exception {
        String keyContent = readFile(path);

        // Remove headers/footers and newlines
        String publicKeyPEM = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    // --- Helper: Read file content ---
    private String readFile(String path) throws Exception {
        try {
            File file = ResourceUtils.getFile("classpath:" + path);
            return new String(Files.readAllBytes(file.toPath()));
        } catch (Exception e) {
            File file = new File(path);
            if (file.exists()) {
                return new String(Files.readAllBytes(file.toPath()));
            } else {
                throw new RuntimeException("Key file not found at: " + path);
            }
        }
    }

}