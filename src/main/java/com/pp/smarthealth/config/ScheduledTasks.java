package com.pp.smarthealth.config;


import com.pp.smarthealth.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ScheduledTasks {

    @Autowired
    private AppointmentService appointmentService;

    @Scheduled(cron = "0 0 8 * * ?") // Every day at 8 AM
    public void sendAppointmentReminders() {
        appointmentService.sendAppointmentReminders();
    }

    @Scheduled(cron = "0 0 9 * * ?") // Every day at 9 AM
    public void sendHealthCheckupAlerts() {
        appointmentService.sendHealthCheckupAlerts();
    }
}
