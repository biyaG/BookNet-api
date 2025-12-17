package it.unipi.booknetapi.shared.lib.configuration;

import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class ConfigurationService {

    private final AppConfiguration config;

    // Constructor Injection
    public ConfigurationService(AppConfiguration config) {
        this.config = config;
    }

}