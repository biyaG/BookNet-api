package it.unipi.booknetapi;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Adds "/api" prefix to all classes annotated with @RestController
        configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));
    }
}
