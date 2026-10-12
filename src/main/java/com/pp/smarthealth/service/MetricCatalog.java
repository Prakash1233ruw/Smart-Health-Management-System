package com.pp.smarthealth.service;

import java.util.*;
import com.pp.smarthealth.exception.BadRequestException;
import com.pp.smarthealth.dto.HealthMetricsDTO;
import org.springframework.beans.BeanWrapperImpl;

public final class MetricCatalog {
    private MetricCatalog() {}
    public static final Map<String,List<String>> UNITS;
    static {
        var units = new LinkedHashMap<String,List<String>>();
        for (String key : List.of("systolicPressure", "diastolicPressure")) units.put(key,List.of("mmHg"));
        units.put("heartRate",List.of("bpm")); units.put("temperature",List.of("°C","°F"));
        units.put("respiratoryRate",List.of("breaths/min")); units.put("bloodOxygenLevel",List.of("%"));
        for (String key : List.of("bloodGlucoseLevel","totalCholesterol","hdlCholesterol","ldlCholesterol","triglycerides")) units.put(key,List.of("mg/dL","mmol/L"));
        units.put("weight",List.of("kg","lb"));
        for (String key : List.of("height","waistCircumference")) units.put(key,List.of("cm","in"));
        units.put("bmi",List.of("kg/m²")); units.put("physicalActivityLevel",List.of("minutes/week"));
        UNITS = Collections.unmodifiableMap(units);
    }
    public static Map<String,String> validate(Map<String,String> units, HealthMetricsDTO dto) {
        if (units == null) return Map.of();
        if (units.size()>UNITS.size()) throw new BadRequestException("Too many measurement units.");
        var bean = new BeanWrapperImpl(dto);
        units.forEach((metric,unit) -> {
            if (!UNITS.containsKey(metric) || !UNITS.get(metric).contains(unit) || bean.getPropertyValue(metric)==null)
                throw new BadRequestException("A unit must match a supported, recorded measurement.");
        });
        return new LinkedHashMap<>(units);
    }
}
