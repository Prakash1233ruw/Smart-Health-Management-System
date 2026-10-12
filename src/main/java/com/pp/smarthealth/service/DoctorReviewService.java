package com.pp.smarthealth.service;

import com.pp.smarthealth.dto.HealthTimelineDtos.*;
import com.pp.smarthealth.exception.*;
import com.pp.smarthealth.model.*;
import com.pp.smarthealth.model.HealthTimelineTypes.ReviewStatus;
import com.pp.smarthealth.repository.*;
import com.pp.smarthealth.security.PatientAccessService;
import jakarta.validation.Validator;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorReviewService {
    private final PatientAccessService access;
    private final HealthChangeAnalysisRepository analyses;
    private final DoctorReviewRepository reviews;
    private final Validator validator;
    public DoctorReviewService(PatientAccessService access, HealthChangeAnalysisRepository analyses,
            DoctorReviewRepository reviews, Validator validator) {
        this.access = access; this.analyses = analyses; this.reviews = reviews; this.validator = validator;
    }
    @Transactional
    public DoctorReviewResponse review(Long id, DoctorReviewRequest request, Authentication auth) {
        Doctor doctor = access.currentDoctor(auth);
        if (id == null || id <= 0 || request == null || !validator.validate(request).isEmpty()) {
            throw new BadRequestException("Invalid review fields or follow-up date.");
        }
        if (request.status() == ReviewStatus.PENDING_REVIEW) { throw new ReviewConflictException(); }
        if (request.status() == ReviewStatus.FOLLOW_UP_REQUIRED
                && (request.followUpRecommendation() == null || request.followUpRecommendation().isBlank())) {
            throw new BadRequestException("A patient-visible recommendation is required for follow-up.");
        }
        try {
            HealthChangeAnalysis analysis = analyses.findForReview(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Analysis not found"));
            access.doctorPatient(analysis.getPatient().getId(), auth);
            ReviewStatus before = analysis.getReviewStatus();
            if (before == ReviewStatus.REVIEWED || before == ReviewStatus.DISMISSED || before == request.status()) {
                throw new ReviewConflictException();
            }
            // PENDING -> any final status; FOLLOW_UP_REQUIRED -> REVIEWED or DISMISSED.
            analysis.setReviewStatus(request.status());
            DoctorReview event = reviews.saveAndFlush(new DoctorReview(analysis, doctor,
                    new DoctorReviewRequest(request.status(), clean(request.internalNotes()),
                            clean(request.followUpRecommendation()), request.recommendedFollowUpDate())));
            analyses.flush();
            return new DoctorReviewResponse(event.getId(), id, event.getReviewerDoctorId(), event.getStatus(),
                    event.getInternalNotes(), event.getFollowUpRecommendation(),
                    event.getRecommendedFollowUpDate(), event.getReviewedAt());
        } catch (org.springframework.dao.OptimisticLockingFailureException ex) { throw new ReviewConflictException(); }
        catch (DataAccessException ex) { throw new TimelineStorageException(); }
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
