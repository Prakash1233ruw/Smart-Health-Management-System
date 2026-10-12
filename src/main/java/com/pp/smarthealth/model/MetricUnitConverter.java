package com.pp.smarthealth.model;

import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MetricUnitConverter implements AttributeConverter<Map<String, String>, String> {
    private static final ObjectMapper JSON = new ObjectMapper();
    public String convertToDatabaseColumn(Map<String, String> value) {
        if (value == null || value.isEmpty()) return null;
        try { return JSON.writeValueAsString(value); }
        catch (Exception ex) { throw new IllegalArgumentException("Invalid measurement units"); }
    }
    public Map<String, String> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return new LinkedHashMap<>();
        try { return JSON.readValue(value, new TypeReference<Map<String,String>>() {}); }
        catch (Exception ex) { throw new IllegalArgumentException("Invalid stored measurement units"); }
    }
}
