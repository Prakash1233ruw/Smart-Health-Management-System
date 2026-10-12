package com.pp.smarthealth.service;

import java.time.LocalDateTime;
import java.util.List;
import com.pp.smarthealth.model.HealthMetrics;
import com.pp.smarthealth.model.HealthTimelineTypes.ChangeDirection;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class HealthChangeCalculatorTest {
    private final HealthChangeCalculator calculator = new HealthChangeCalculator();

    @Test void symptomsAreNormalizedAndComparedWithoutCaseOrDuplicateNoise() {
        var result = calculator.symptoms(List.of(" COUGH ", "Fever", "fever", "short   breath"),
                List.of("cough", "fatigue"));
        assertThat(result.newSymptoms()).containsExactly("Fever", "short breath");
        assertThat(result.persistentSymptoms()).containsExactly("COUGH");
        assertThat(result.resolvedSymptoms()).containsExactly("fatigue");
        assertThat(result.previousRecordAvailable()).isTrue();
    }
    @Test void absentBaselineAndExplicitEmptySymptomsAreDifferent() {
        assertThat(calculator.symptoms(List.of("cough"), null).previousRecordAvailable()).isFalse();
        assertThat(calculator.symptoms(List.of(), List.of("cough")).resolvedSymptoms()).containsExactly("cough");
        assertThatThrownBy(() -> calculator.normalize(List.of(" "))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void calculationsSortRecordsAndKeepBloodPressureComponentsSeparate() {
        var previous = metric(1L, 1, 60);
        previous.setWeight(80.0); previous.setSystolicPressure(120); previous.setDiastolicPressure(80);
        var latest = metric(2L, 2, 75);
        latest.setWeight(78.0); latest.setSystolicPressure(125); latest.setDiastolicPressure(78);
        var changes = calculator.metrics(List.of(latest, previous));
        var heart = changes.stream().filter(c -> c.metric().equals("heartRate")).findFirst().orElseThrow();
        assertThat(heart.absoluteChange()).isEqualByComparingTo("15");
        assertThat(heart.percentageChange()).isEqualByComparingTo("25.00");
        assertThat(heart.direction()).isEqualTo(ChangeDirection.INCREASED);
        assertThat(heart.unit()).isEqualTo("NOT_RECORDED");
        assertThat(changes.stream().filter(c -> c.metric().equals("diastolicPressure")).findFirst().orElseThrow().absoluteChange())
                .isEqualByComparingTo("-2");
        assertThat(changes.stream().filter(c -> c.metric().equals("weight")).findFirst().orElseThrow().direction())
                .isEqualTo(ChangeDirection.DECREASED);
    }
    @Test void zeroBaselineDoesNotDivideAndMissingLatestDoesNotReuseOldReading() {
        var previous = metric(1L, 1, 0);
        var latest = metric(2L, 2, 4);
        assertThat(calculator.metrics(List.of(previous, latest)).stream().filter(c -> c.metric().equals("heartRate"))
                .findFirst().orElseThrow().percentageChange()).isNull();
        latest.setHeartRate(null);
        var result = calculator.metrics(List.of(previous, latest)).stream().filter(c -> c.metric().equals("heartRate"))
                .findFirst().orElseThrow();
        assertThat(result.latestValue()).isNull();
        assertThat(result.absoluteChange()).isNull();
        assertThat(result.direction()).isEqualTo(ChangeDirection.INSUFFICIENT_DATA);
    }
    @Test void sameTimeReadingsDoNotCreateFalseTrendsAndNonFiniteValuesAreIgnored() {
        var first = metric(1L, 1, 60);
        var second = metric(2L, 1, 70);
        second.setWeight(Double.NaN);
        var result = calculator.metrics(List.of(first, second)).stream().filter(c -> c.metric().equals("heartRate"))
                .findFirst().orElseThrow();
        assertThat(result.latestValue()).isEqualByComparingTo("70");
        assertThat(result.previousValue()).isNull();
        assertThat(calculator.values(second)).doesNotContainKey("weight");
        assertThat(calculator.metrics(List.of())).allMatch(c -> c.direction() == ChangeDirection.INSUFFICIENT_DATA);
    }
    @Test void percentagesRoundAndStableReadingsRemainStable() {
        var first = metric(1L, 1, 3);
        var second = metric(2L, 2, 4);
        first.setWeight(80.0); second.setWeight(80.0);
        var changes = calculator.metrics(List.of(first, second));
        assertThat(changes.stream().filter(c -> c.metric().equals("heartRate")).findFirst().orElseThrow().percentageChange())
                .isEqualByComparingTo("33.33");
        assertThat(changes.stream().filter(c -> c.metric().equals("weight")).findFirst().orElseThrow().direction())
                .isEqualTo(ChangeDirection.STABLE);
    }
    @Test void incompatibleOrMissingUnitsSuppressComparisonsWithoutConversion() {
        var previous=metric(1L,1,60);var latest=metric(2L,2,75);
        previous.setUnits(java.util.Map.of("heartRate","bpm"));
        var result=calculator.metrics(java.util.List.of(previous,latest)).stream().filter(m->m.metric().equals("heartRate")).findFirst().orElseThrow();
        assertThat(result.absoluteChange()).isNull();assertThat(result.previousUnit()).isEqualTo("bpm");
        latest.setUnits(java.util.Map.of("heartRate","bpm"));
        assertThat(calculator.metrics(java.util.List.of(previous,latest)).stream().filter(m->m.metric().equals("heartRate")).findFirst().orElseThrow().absoluteChange()).isEqualByComparingTo("15");
        previous.setBloodGlucoseLevel(100);latest.setBloodGlucoseLevel(5.2);
        previous.setUnits(java.util.Map.of("bloodGlucoseLevel","mg/dL"));latest.setUnits(java.util.Map.of("bloodGlucoseLevel","mmol/L"));
        var glucose=calculator.metrics(java.util.List.of(previous,latest)).stream().filter(m->m.metric().equals("bloodGlucoseLevel")).findFirst().orElseThrow();
        assertThat(glucose.direction()).isEqualTo(ChangeDirection.INSUFFICIENT_DATA);assertThat(glucose.absoluteChange()).isNull();assertThat(glucose.unit()).isEqualTo("mmol/L");
    }
    private HealthMetrics metric(Long id, int day, Integer heart) {
        var metric = new HealthMetrics();
        metric.setId(id); metric.setTimestamp(LocalDateTime.of(2026, 1, day, 10, 0)); metric.setHeartRate(heart);
        return metric;
    }
}
