package com.helpflow.supportticket.infrastructure.config;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;
import com.helpflow.supportticket.domain.model.Category;
import com.helpflow.supportticket.domain.model.Priority;
import com.helpflow.supportticket.domain.model.TicketStatus;
import com.helpflow.supportticket.domain.model.UserStatus;
import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

/**
 * Enum values are written in Spanish as in the specification (e.g. "CRÍTICA"). Clients may omit the
 * accent or change the case, both in JSON bodies and in query parameters.
 */
@Configuration
class EnumJacksonConfig {

    @Bean
    SimpleModule lenientEnumModule() {
        SimpleModule module = new SimpleModule("lenient-enums");
        module.addDeserializer(Category.class, new LenientEnumDeserializer<>(Category::valueOf));
        module.addDeserializer(Priority.class, new LenientEnumDeserializer<>(Priority::valueOf));
        module.addDeserializer(TicketStatus.class, new LenientEnumDeserializer<>(TicketStatus::valueOf));
        module.addDeserializer(UserStatus.class, new LenientEnumDeserializer<>(UserStatus::valueOf));
        return module;
    }

    @Bean
    Converter<String, Priority> priorityParamConverter() {
        return source -> Priority.valueOf(normalize(source));
    }

    @Bean
    Converter<String, Category> categoryParamConverter() {
        return source -> Category.valueOf(normalize(source));
    }

    @Bean
    Converter<String, TicketStatus> statusParamConverter() {
        return source -> TicketStatus.valueOf(normalize(source));
    }

    static String normalize(String raw) {
        String stripped = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return stripped.toUpperCase(Locale.ROOT);
    }

    private static final class LenientEnumDeserializer<E extends Enum<E>> extends ValueDeserializer<E> {

        private final Function<String, E> parser;

        private LenientEnumDeserializer(Function<String, E> parser) {
            this.parser = parser;
        }

        @Override
        @SuppressWarnings("unchecked")
        public E deserialize(JsonParser p, DeserializationContext ctxt) {
            String raw = p.getValueAsString();
            if (raw == null || raw.isBlank()) {
                return null;
            }
            try {
                return parser.apply(normalize(raw));
            } catch (IllegalArgumentException ex) {
                return (E) ctxt.handleWeirdStringValue(handledType(), raw, "unknown value");
            }
        }
    }
}
