package it.unipi.booknetapi.shared.lib.database;

import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import lombok.Getter;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;

@Component
@Getter
public class Neo4jManager {

    private final Driver driver;

    public Neo4jManager(AppConfig config) {
        System.out.println("Initializing Neo4j Driver...");

        this.driver = GraphDatabase.driver(
                config.getNeo4jUri(),
                AuthTokens.basic(config.getNeo4jUser(), config.getNeo4jPassword())
        );
    }

    public Session getSession() {
        return driver.session();
    }

    @PreDestroy
    public void close() {
        System.out.println("Closing Neo4j Driver...");
        if (driver != null) {
            driver.close();
        }
    }

}
