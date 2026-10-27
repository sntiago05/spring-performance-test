package com.helpflow.dispatch.infrastructure.config;

import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Messages are served in English (default) or Spanish depending on the Accept-Language header.
 */
@Configuration
class LocaleConfig {

    @Bean
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.of("es")));
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }
}
