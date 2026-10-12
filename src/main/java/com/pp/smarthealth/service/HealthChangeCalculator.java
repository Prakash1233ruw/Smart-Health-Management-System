package com.pp.smarthealth.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import com.pp.smarthealth.dto.HealthTimelineDtos.*;
import com.pp.smarthealth.model.HealthMetrics;
import com.pp.smarthealth.model.HealthTimelineTypes.ChangeDirection;
import org.springframework.stereotype.Component;

/** Pure calculations: no provider, clinical thresholds, or diagnosis rules. */
@Component
public class HealthChangeCalculator {
    private static final Map<String, Function<HealthMetrics, Number>> FIELDS = new LinkedHashMap<>();
    static {
        FIELDS.put("systolicPressure", HealthMetrics::getSystolicPressure);
        FIELDS.put("diastolicPressure", HealthMetrics::getDiastolicPressure);
        FIELDS.put("heartRate", HealthMetrics::getHeartRate);
        FIELDS.put("temperature", HealthMetrics::getTemperature);
        FIELDS.put("respiratoryRate", HealthMetrics::getRespiratoryRate);
        FIELDS.put("bloodOxygenLevel", HealthMetrics::getBloodOxygenLevel);
        FIELDS.put("bloodGlucoseLevel", HealthMetrics::getBloodGlucoseLevel);
        FIELDS.put("weight", HealthMetrics::getWeight);
        FIELDS.put("height", HealthMetrics::getHeight);
        FIELDS.put("bmi", HealthMetrics::getBmi);
        FIELDS.put("totalCholesterol", HealthMetrics::getTotalCholesterol);
        FIELDS.put("hdlCholesterol", HealthMetrics::getHdlCholesterol);
        FIELDS.put("ldlCholesterol", HealthMetrics::getLdlCholesterol);
        FIELDS.put("triglycerides", HealthMetrics::getTriglycerides);
        FIELDS.put("waistCircumference", HealthMetrics::getWaistCircumference);
        FIELDS.put("physicalActivityLevel", HealthMetrics::getPhysicalActivityLevel);
    }

    public List<String> normalize(List<String> values) { return List.copyOf(symptomSet(values).values()); }
    private Map<String, String> symptomSet(List<String> values) {
        Map<String, String> set = new LinkedHashMap<>();
        if (values == null) { return set; }
        for (String value : values) {
            if (value == null || value.isBlank()) { throw new IllegalArgumentException("Blank symptom"); }
            String display = value.trim().replaceAll("\\s+", " ");
            set.putIfAbsent(display.toLowerCase(Locale.ROOT), display);
        }
        return set;
    }
    public SymptomChanges symptoms(List<String> current, List<String> previous) {
        Map<String, String> now = symptomSet(current);
        Map<String, String> before = symptomSet(previous);
        return new SymptomChanges(
                now.entrySet().stream().filter(e -> !before.containsKey(e.getKey())).map(Map.Entry::getValue).toList(),
                now.entrySet().stream().filter(e -> before.containsKey(e.getKey())).map(Map.Entry::getValue).toList(),
                before.entrySet().stream().filter(e -> !now.containsKey(e.getKey())).map(Map.Entry::getValue).toList(),
                previous != null);
    }

    public List<MetricChange> metrics(List<HealthMetrics> records) {
        List<HealthMetrics> ordered = records.stream()
                .filter(r -> r.getTimestamp() != null)
                .sorted(Comparator.comparing(HealthMetrics::getTimestamp).reversed()
                        .thenComparing(HealthMetrics::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        HealthMetrics latest = ordered.isEmpty() ? null : ordered.get(0);
        List<MetricChange> changes = new ArrayList<>();
        FIELDS.forEach((name, getter) -> {
            // The most recent record remains the latest even if its particular field is null.
            BigDecimal latestValue = latest == null ? null : decimal(getter.apply(latest));
            HealthMetrics previous = ordered.stream().skip(1)
                    .filter(r -> latest != null && r.getTimestamp().isBefore(latest.getTimestamp()))
                    .filter(r -> decimal(getter.apply(r)) != null).findFirst().orElse(null);
            BigDecimal previousValue = previous == null ? null : decimal(getter.apply(previous));
            String unit = unit(latest, name);
            String previousUnit = unit(previous, name);
            boolean comparableUnits = unit.equals(previousUnit);
            BigDecimal absolute = latestValue == null || previousValue == null || !comparableUnits ? null : latestValue.subtract(previousValue);
            BigDecimal percentage = absolute == null || previousValue.signum() == 0 ? null
                    : absolute.multiply(BigDecimal.valueOf(100)).divide(previousValue, 2, RoundingMode.HALF_UP);
            ChangeDirection direction = absolute == null ? ChangeDirection.INSUFFICIENT_DATA
                    : absolute.signum() > 0 ? ChangeDirection.INCREASED
                    : absolute.signum() < 0 ? ChangeDirection.DECREASED : ChangeDirection.STABLE;
            changes.add(new MetricChange(name, unit, previousValue, latestValue, absolute,
                    percentage, direction, previous == null ? null : previous.getTimestamp(),
                    latest == null ? null : latest.getTimestamp(), previousUnit));
        });
        return List.copyOf(changes);
    }

    public Map<String, BigDecimal> values(HealthMetrics record) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        FIELDS.forEach((name, getter) -> {
            BigDecimal value = decimal(getter.apply(record));
            if (value != null) { values.put(name, value); }
        });
        return Collections.unmodifiableMap(values);
    }
    private BigDecimal decimal(Number value) {
        if (value == null || !Double.isFinite(value.doubleValue())) { return null; }
        return new BigDecimal(value.toString());
    }
    private String unit(HealthMetrics record, String metric) {
        return record == null || record.getUnits() == null ? "NOT_RECORDED" : record.getUnits().getOrDefault(metric,"NOT_RECORDED");
    }
}
