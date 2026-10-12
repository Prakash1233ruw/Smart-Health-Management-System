package com.pp.smarthealth.service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.security.MessageDigest;
import com.pp.smarthealth.dto.ReportImportDtos.*;
import com.pp.smarthealth.dto.HealthMetricsDTO;
import com.pp.smarthealth.exception.*;
import com.pp.smarthealth.model.*;
import com.pp.smarthealth.repository.*;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportImportService {
    private final ReportImportDraftRepository drafts; private final DoctorRepository doctors;
    private final PatientRepository patients; private final ReportDocumentReader reader;
    private final ReportExtractionClient client; private final ClinicalJson json; private final HealthMetricsService metrics;
    public ReportImportService(ReportImportDraftRepository drafts,DoctorRepository doctors,PatientRepository patients,
        ReportDocumentReader reader,ReportExtractionClient client,ClinicalJson json,HealthMetricsService metrics) {
        this.drafts=drafts;this.doctors=doctors;this.patients=patients;this.reader=reader;this.client=client;this.json=json;this.metrics=metrics;
    }
    public DraftView upload(Long patientId,MultipartFile file,Authentication auth) {
        Doctor doctor=doctor(auth);
        var patient=patients.findById(patientId).orElseThrow(()->new ResourceNotFoundException("Patient not found"));
        var document=reader.read(file);
        var extraction=json.narrative(client.extract(document),Extraction.class);
        if(extraction.rows().isEmpty()) throw new BadRequestException("No measurement results were extracted. Try a clearer report or enter measurements manually.");
        List<String> warnings=new ArrayList<>(extraction.warnings());
        LocalDate date=null;
        if(extraction.reportDate()!=null && !extraction.reportDate().isBlank()) {
            try { date=LocalDate.parse(extraction.reportDate());if(date.isAfter(LocalDate.now())) { date=null;warnings.add("The extracted date is in the future. Confirm the actual collection date."); } }
            catch(Exception ex) { warnings.add("The report date could not be read. Enter the collection date after checking the report."); }
        }
        if(date==null) warnings.add("A collection date must be supplied by the reviewing doctor.");
        var rows=new ArrayList<DraftRow>();
        for(int i=0;i<extraction.rows().size();i++) {
            var row=extraction.rows().get(i);
            boolean sourceFound=document.mode().equals("PDF_TEXT") && row.page()<=document.pageText().size()
                && !row.sourceText().isBlank() && normalized(document.pageText().get(row.page()-1)).contains(normalized(row.sourceText()));
            rows.add(new DraftRow(i,row.metric(),row.value(),row.unit(),row.sourceText(),row.page(),
                MetricCatalog.UNITS.containsKey(row.metric()),sourceFound?"TEXT_MATCHED":document.images().isEmpty()?"UNVERIFIED":"VISUAL_REVIEW_REQUIRED"));
        }
        warnings.add("Extracted values and excerpts are unconfirmed. Verify patient identity, dates, result values (not reference ranges), and units against the original report.");
        var draft=new ReportImportDraft();draft.setDoctorId(doctor.getId());draft.setPatientId(patient.getId());draft.setCreatedAt(LocalDateTime.now());
        String name=Optional.ofNullable(file.getOriginalFilename()).orElse("report").replaceAll("[\\\\/\\p{Cntrl}]","_");
        draft.setFilename(name.substring(0,Math.min(160,name.length())));
        try { draft.setSourceSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file.getBytes()))); }
        catch(Exception ex) { throw new BadRequestException("Unable to read the report."); }
        draft.setDraftJson(json.write(new DraftView(null,patientId,patient.getName(),draft.getFilename(),date,rows,warnings,MetricCatalog.UNITS,"DRAFT",null)));
        return view(drafts.save(draft));
    }
    @Transactional(readOnly=true)
    public DraftView get(Long id,Authentication auth) { return view(owned(drafts.findById(id).orElseThrow(()->new ResourceNotFoundException("Report draft not found")),auth)); }
    @Transactional
    public HealthMetricsDTO confirm(Long id,ConfirmRequest request,Authentication auth) {
        var draft=owned(drafts.lockById(id).orElseThrow(()->new ResourceNotFoundException("Report draft not found")),auth);
        if(!request.verifiedAgainstReport()) throw new BadRequestException("Verify the report before saving.");
        if(draft.getMeasurementId()!=null) {
            if(!json.write(request).equals(draft.getConfirmedRequestJson())) throw new BadRequestException("This draft was already confirmed. It cannot be changed or saved again.");
            return metrics.getHealthMetricsById(draft.getMeasurementId());
        }
        if(draft.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) throw new BadRequestException("This draft expired. Upload the report again.");
        var original=view(draft);
        var dto=new HealthMetricsDTO();dto.setPatientId(draft.getPatientId());dto.setDate(request.recordedDate());dto.setTimestamp(request.recordedDate().atStartOfDay());
        var units=new LinkedHashMap<String,String>();var indices=new HashSet<Integer>();var bean=new BeanWrapperImpl(dto);
        for(var row:request.rows()) {
            if(row.index()<0 || row.index()>=original.rows().size() || !indices.add(row.index()) || units.containsKey(row.metric())
                || !MetricCatalog.UNITS.containsKey(row.metric()) || !MetricCatalog.UNITS.get(row.metric()).contains(row.unit()))
                throw new BadRequestException("Select each supported measurement once and choose its recorded unit.");
            if(row.value().scale()>6) throw new BadRequestException("Measurement values support up to six decimal places.");
            try {
                Class<?> type=bean.getPropertyType(row.metric());
                bean.setPropertyValue(row.metric(),type==Integer.class?row.value().intValueExact():row.value().doubleValue());
            } catch(Exception ex) { throw new BadRequestException("This measurement requires a valid whole-number result. Do not round an extracted reading; exclude it if unsupported."); }
            units.put(row.metric(),row.unit());
        }
        dto.setUnits(units);
        var saved=metrics.saveHealthMetrics(dto);
        draft.setStatus("CONFIRMED");draft.setMeasurementId(saved.getId());draft.setConfirmedAt(LocalDateTime.now());draft.setConfirmedRequestJson(json.write(request));drafts.save(draft);
        return saved;
    }
    private Doctor doctor(Authentication auth) {
        if(auth==null || auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_DOCTOR"))) throw new AccessDeniedException("Only doctors can import reports.");
        return doctors.findByUsername(auth.getName()).orElseThrow(()->new AccessDeniedException("Doctor account unavailable."));
    }
    private ReportImportDraft owned(ReportImportDraft draft,Authentication auth) {
        if(!draft.getDoctorId().equals(doctor(auth).getId())) throw new AccessDeniedException("This report draft belongs to another doctor.");
        return draft;
    }
    private DraftView view(ReportImportDraft draft) {
        var snapshot=json.stored(draft.getDraftJson(),DraftView.class);
        return new DraftView(draft.getId(),draft.getPatientId(),snapshot.patientName(),draft.getFilename(),snapshot.reportDate(),snapshot.rows(),snapshot.warnings(),MetricCatalog.UNITS,draft.getStatus(),draft.getMeasurementId());
    }
    private String normalized(String text) { return text.replaceAll("\\s+"," ").trim(); }
}
