package it.unipi.booknetapi.shared.lib.database;

import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class Neo4jManager {

    private final AppConfig appConfig;

    public Neo4jManager(AppConfig config) {
        // System.out.println("Initializing Neo4j Driver...");

        /*this.driver = GraphDatabase.driver(
                config.getNeo4jUri(),
                AuthTokens.basic(config.getNeo4jUser(), config.getNeo4jPassword())
        );*/

        this.appConfig = config;
    }

    @Bean(destroyMethod = "close")
    public Driver getDriver() {
        // System.out.println("Initializing Neo4J Driver via @Bean...");

        return GraphDatabase.driver(
                appConfig.getNeo4jUri(),
                AuthTokens.basic(appConfig.getNeo4jUser(), appConfig.getNeo4jPassword())
        );
    }

}
