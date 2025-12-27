package it.unipi.booknetapi.shared.lib.database;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import com.mongodb.MongoClientSettings;
import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MongoManager {

    private final AppConfig appConfig;


    public MongoManager(AppConfig appConfig) {
        // System.out.println("Initializing MongoDB Connection...");

        this.appConfig = appConfig;

        /*
        // Create the connection and STORE it in the field
        this.mongoClient = MongoClients.create(appConfig.getMongoUri());

        // Select the database and STORE it
        this.database = mongoClient.getDatabase(appConfig.getMongoDatabase());
        */

        /*
        // 1. Define POJO Codec Registry
        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        this.mongoClient = MongoClients.create(appConfig.getMongoUri());

        // 2. Apply Codec Registry when getting the database
        this.database = mongoClient.getDatabase(appConfig.getMongoDatabase())
                .withCodecRegistry(codecRegistry);
        */
    }

    @Bean
    public MongoClient getMongoClient(MeterRegistry meterRegistry) {
        // System.out.println("Initializing Mongo Client via @Bean...");

        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(appConfig.getMongoUri()))
                .codecRegistry(codecRegistry)
                .addCommandListener(new MongoMetricsCommandListener(meterRegistry))
                .build();

        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabase getDatabase(MongoClient mongoClient) {
        // System.out.println("Initializing Mongo Database via @Bean...");

        return mongoClient.getDatabase(appConfig.getMongoDatabase());
    }

}