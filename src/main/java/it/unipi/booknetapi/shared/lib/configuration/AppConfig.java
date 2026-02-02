package it.unipi.booknetapi.shared.lib.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private String name;

    private String privateKeyPath;
    private String publicKeyPath;

    private String encryptionKey;

    private String mongoUri;
    private String mongoUser;
    private String mongoPassword;
    private String mongoDatabase;

    private String neo4jUri;
    private String neo4jUser;
    private String neo4jPassword;
    private Boolean neo4jEncrypted;
    private String neo4jDatabase;

//    private String redisUri;
//    private String redisHost;
//    private Integer redisPort;
//    private String redisPassword;
//    private Integer redisTimeout;
//    private Long redisDefaultExpiration;
//    private String redisAppSpaceName;

    private Integer batchSize;

}