package it.unipi.booknetapi.shared.lib.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private String name = "Default App";

    private String privateKeyPath = "keys/private_key_pkcs8.pem";
    private String publicKeyPath = "keys/public_key.pem";

    private String encryptionKey = "12345678901234567890123456789012";

    private String mongoUri = "mongodb://localhost:27017";
    private String mongoUser = "root";
    private String mongoPassword = "";
    private String mongoDatabase = "booknet";

    private String neo4jUri = "neo4j://localhost:7687";
    private String neo4jUser = "neo4j";
    private String neo4jPassword = "";
    private Boolean neo4jEncrypted = false;
    private String neo4jDatabase = "neo4j";

    private String redisUri = "redis://localhost:6379";
    private String redisHost = "localhost";
    private Integer redisPort = 6379;
    private String redisPassword = "";
    private Integer redisTimeout = 2000;
    private Long redisDefaultExpiration = 3600L;
    private String redisAppSpaceName = "booknet";

    private Integer batchSize = 100;

}