package it.unipi.booknetapi.shared.lib.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import com.mongodb.MongoClientSettings;
import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import lombok.Getter;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;

@Component
@Getter
public class MongoManager {

    private final MongoClient mongoClient;
    private final MongoDatabase database;


    public MongoManager(AppConfig appConfig) {
        System.out.println("Initializing MongoDB Connection...");

        /*
        // Create the connection and STORE it in the field
        this.mongoClient = MongoClients.create(appConfig.getMongoUri());

        // Select the database and STORE it
        this.database = mongoClient.getDatabase(appConfig.getMongoDatabase());
        */

        // 1. Define POJO Codec Registry
        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        this.mongoClient = MongoClients.create(appConfig.getMongoUri());

        // 2. Apply Codec Registry when getting the database
        this.database = mongoClient.getDatabase(appConfig.getMongoDatabase())
                .withCodecRegistry(codecRegistry);
    }

    @PreDestroy
    public void close() {
        System.out.println("Closing MongoDB Connection...");
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

}