package com.bookolab.restservice.cron;

import com.bookolab.restservice.service.PageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Truncate {

    private final PageService pageService;

    public Truncate(PageService pageService) {
        this.pageService = pageService;
    }

    // Runs at the start of every minute (0th second)
    @Scheduled(cron = "0 */10 * * * *")
    public void executeTask() {
        pageService.globalTruncateAndRepaginate(500);
        // System.out.println("Cron task executed successfully!");
    }
}
