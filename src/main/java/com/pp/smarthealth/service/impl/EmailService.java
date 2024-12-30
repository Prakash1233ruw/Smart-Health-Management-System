package com.pp.smarthealth.service.impl;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    @Value("{spring.mail.username}")
	private String formId;
    
    @Autowired
    private TemplateEngine templateEngine;

    
    public void sendEmailWithTemplate(String to, String subject, String templateName, Map<String, Object> templateModel) throws MessagingException {
        Context context = new Context();
        context.setVariables(templateModel);

        if (templateModel.containsKey("appointmentDateTime")) {
            String appointmentDateTimeStr = (String) templateModel.get("appointmentDateTime");
            LocalDateTime appointmentDateTime = LocalDateTime.parse(appointmentDateTimeStr);
            String formattedDateTime = appointmentDateTime.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm"));
            templateModel.put("appointmentDateTime", formattedDateTime);
        }
        
        String htmlContent = templateEngine.process(templateName, context);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
        helper.setText(htmlContent, true);
        helper.setTo(to);
        
        helper.setSubject(subject);
        helper.setFrom(formId);

        mailSender.send(mimeMessage);
        
        
    }
}


