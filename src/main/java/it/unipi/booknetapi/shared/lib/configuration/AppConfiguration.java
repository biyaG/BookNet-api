package it.unipi.booknetapi.shared.lib.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AppConfiguration {

    @Autowired
    private Environment env;


    // --- String ---
    public String getString(String key) {
        return env.getProperty(key);
    }

    public String getString(String key, String defaultValue) {
        return env.getProperty(key, defaultValue);
    }

    // --- Integer ---
    public Integer getInt(String key) {
        return env.getProperty(key, Integer.class);
    }

    public Integer getInt(String key, Integer defaultValue) {
        return env.getProperty(key, Integer.class, defaultValue);
    }

    // --- Boolean ---
    public Boolean getBoolean(String key) {
        return env.getProperty(key, Boolean.class);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        return env.getProperty(key, Boolean.class, defaultValue);
    }

    // --- Long ---
    public Long getLong(String key) {
        return env.getProperty(key, Long.class);
    }

    public Long getLong(String key, Long defaultValue) {
        return env.getProperty(key, Long.class, defaultValue);
    }

    // --- Float ---
    public Float getFloat(String key) {
        return env.getProperty(key, Float.class);
    }

    public Float getFloat(String key, Float defaultValue) {
        return env.getProperty(key, Float.class, defaultValue);
    }

    // --- Double ---
    public Double getDouble(String key) {
        return env.getProperty(key, Double.class);
    }

    public Double getDouble(String key, Double defaultValue) {
        return env.getProperty(key, Double.class, defaultValue);
    }

}