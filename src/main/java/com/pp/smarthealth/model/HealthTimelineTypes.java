package com.pp.smarthealth.model;

/** Vocabulary shared by persistence and API DTOs. */
public final class HealthTimelineTypes {
    private HealthTimelineTypes() { }
    public enum ReviewStatus { PENDING_REVIEW, REVIEWED, FOLLOW_UP_REQUIRED, DISMISSED }
    public enum ChangeDirection { INCREASED, DECREASED, STABLE, INSUFFICIENT_DATA }
    public enum NarrativeStatus { AVAILABLE, UNAVAILABLE, NOT_REQUESTED }
    public enum TimelineEventType {
        HEALTH_METRICS_RECORDED, SYMPTOM_ANALYSIS, AI_CHANGE_ANALYSIS,
        DOCTOR_REVIEW, FOLLOW_UP_RECOMMENDED
    }
}
