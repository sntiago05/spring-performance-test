package com.helpflow.dispatch.infrastructure.config;

import com.helpflow.dispatch.domain.model.Category;
import com.helpflow.dispatch.domain.model.Priority;
import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleModule;

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

    private static final class LenientEnumDeserializer<E extends Enum<E>> extends ValueDeserializer<E> {

        private final Function<String, E> parser;

        private LenientEnumDeserializer(Function<String, E> parser) {
            this.parser = parser;
        }

        @Override
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
