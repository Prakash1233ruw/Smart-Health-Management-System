package com.pp.smarthealth.service;

public interface ReportExtractionClient {
    String extract(ReportDocumentReader.Document document);
}
