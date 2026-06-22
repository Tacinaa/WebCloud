package com.utopios.tickets.report_job;

import com.utopios.tickets.report_job.service.ReportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReportJobApplication implements CommandLineRunner {

    private final ReportService reportService;

    public ReportJobApplication(ReportService reportService) {
        this.reportService = reportService;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(ReportJobApplication.class, args)));
    }

    @Override
    public void run(String... args) {
        reportService.generateAndExport();
    }
}
