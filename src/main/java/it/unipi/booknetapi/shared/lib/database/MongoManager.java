package it.unipi.booknetapi.shared.lib.database;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import com.mongodb.MongoClientSettings;
import it.unipi.booknetapi.shared.lib.configuration.AppConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
public class MongoManager {

    private final AppConfig appConfig;


    public MongoManager(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Bean
    public MongoClient getMongoClient(MeterRegistry meterRegistry) {
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromProviders(
                PojoCodecProvider.builder()
                        .automatic(true)
                        /*.conventions(List.of(
                                Conventions.ANNOTATION_CONVENTION, // Ensures @BsonDiscriminator is read
                                Conventions.CLASS_AND_PROPERTY_CONVENTION
                        ))*/
                        .register(
                                "it.unipi.booknetapi.model.author",
                                "it.unipi.booknetapi.model.book",
                                "it.unipi.booknetapi.model.fetch",
                                "it.unipi.booknetapi.model.genre",
                                "it.unipi.booknetapi.model.notification",
                                "it.unipi.booknetapi.model.review",
                                "it.unipi.booknetapi.model.stat",
                                "it.unipi.booknetapi.model.user"
                        )
                        .build()
        );

        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(appConfig.getMongoUri()))
                .codecRegistry(codecRegistry)
                .addCommandListener(new MongoMetricsCommandListener(meterRegistry))
                .build();

        return MongoClients.create(settings);
    }

    @Bean
    public MongoDatabase getDatabase(MongoClient mongoClient) {
        return mongoClient.getDatabase(appConfig.getMongoDatabase());
    }

}