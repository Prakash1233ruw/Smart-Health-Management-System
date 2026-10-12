package com.pp.smarthealth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.pp.smarthealth.exception.AIServiceException;
import com.pp.smarthealth.exception.TimelineStorageException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/** Shared strict validation for the new narrative and bounded stored snapshots. */
@Component
public class ClinicalJson {
    private final ObjectMapper mapper;
    private final ObjectMapper strict;
    private final Validator validator;
    public ClinicalJson(ObjectMapper mapper, Validator validator) {
        this.mapper = mapper;
        this.validator = validator;
        strict = JsonMapper.builder().findAndAddModules()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .withCoercionConfig(LogicalType.Textual, c -> c
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)).build();
    }
    public <T> T narrative(String raw, Class<T> type) {
        if (raw == null || raw.isBlank() || raw.length() > 24000) { throw AIServiceException.invalidResponse(); }
        try {
            T value = strict.readValue(raw, type);
            if (value == null || !validator.validate(value).isEmpty()) { throw AIServiceException.invalidResponse(); }
            return value;
        } catch (JsonProcessingException ex) { throw AIServiceException.invalidResponse(); }
    }
    public String write(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new TimelineStorageException(); }
    }
    public <T> T stored(String raw, Class<T> type) {
        try {
            if (raw == null || raw.length() > 250000) { throw new TimelineStorageException(); }
            T value = mapper.readValue(raw, type);
            if (value == null) { throw new TimelineStorageException(); }
            return value;
        } catch (JsonProcessingException ex) { throw new TimelineStorageException(); }
    }
    public java.util.List<String> symptoms(String raw) {
        String[] values = stored(raw, String[].class);
        if (values.length > 20 || java.util.Arrays.stream(values)
                .anyMatch(v -> v == null || v.isBlank() || v.length() > 200)) { throw new TimelineStorageException(); }
        return java.util.List.of(values);
    }
}
