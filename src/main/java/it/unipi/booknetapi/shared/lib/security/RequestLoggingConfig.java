package it.unipi.booknetapi.shared.lib.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();

        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true); // Logs the body (e.g. JSON)
        filter.setMaxPayloadLength(10000); // Limit body size to avoid huge logs
        filter.setIncludeHeaders(false); // Enable if you need headers
        filter.setAfterMessagePrefix("REQUEST HTTP: ");

        return filter;
    }
}
