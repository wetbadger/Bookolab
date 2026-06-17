package com.example.restservice.cron;

import com.example.restservice.service.PageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Truncate {

    private final PageService pageService;

    public Truncate(PageService pageService) {
        this.pageService = pageService;
    }

    // Runs at the start of every minute (0th second)
    @Scheduled(cron = "0 * * * * *")
    public void executeTask() {
        pageService.globalTruncateAndRepaginate(50);
        System.out.println("Cron task executed successfully!");
    }
}
