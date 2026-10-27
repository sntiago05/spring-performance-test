package com.helpflow.dispatch.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.helpflow.dispatch.domain.model.Category;
import com.helpflow.dispatch.domain.model.Priority;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The specification writes the top priority as "CRÍTICA" (accented). Clients may send it with or
 * without the accent and in any case, so enums are parsed leniently.
 */
@Configuration
class EnumJacksonConfig {

    @Bean
    SimpleModule lenientEnumModule() {
        SimpleModule module = new SimpleModule("lenient-enums");
        module.addDeserializer(Category.class, new LenientEnumDeserializer<>(Category::valueOf));
        module.addDeserializer(Priority.class, new LenientEnumDeserializer<>(Priority::valueOf));
        return module;
    }

    static String normalize(String raw) {
        String stripped = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return stripped.toUpperCase(Locale.ROOT);
    }

    private static final class LenientEnumDeserializer<E extends Enum<E>> extends JsonDeserializer<E> {

        private final Function<String, E> parser;

        private LenientEnumDeserializer(Function<String, E> parser) {
            this.parser = parser;
        }

        @Override
        public E deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
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
