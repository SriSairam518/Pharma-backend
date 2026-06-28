package com.sairam.pharma.service;

// ================================================================
// CloudinaryCleanupService.java  —  AUTOMATED CLEANUP JOB
//
// WHAT IT DOES:
// Runs automatically every hour.
// Finds all Cloudinary images tagged "pharma_temp" AND older than 2 hours.
// Deletes them one by one.
//
// WHY 2 HOURS?
// A normal user flow takes < 5 minutes (upload → OCR → review → save).
// 2 hours is a very generous buffer — even if someone leaves the form
// open for a long time before cancelling, the image gets cleaned up.
// Any image that has been sitting in "temp" for 2+ hours was definitely
// abandoned (OCR failed, user closed the tab, bill save errored, etc).
//
// HOW IT WORKS:
// @Scheduled(cron = "0 0 * * * *") = "at minute 0 of every hour"
// Spring calls this automatically — no external scheduler needed.
//
// SAFETY:
// Once a bill/payment is saved, confirmFile() removes the TEMP_TAG.
// So even if cleanup runs while someone is actively saving,
// it will never delete a confirmed (saved) image — the tag is gone.
// ================================================================

import com.sairam.pharma.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryCleanupService {

    private final FileStorageService fileStorageService;

    @Scheduled(cron = "0 0 * * * *")
    public void deleteOrphanedTempImages() {
        log.info("Cloudinary cleanup job started — looking for temp images older than 2 hours");

        try {
            List<Map<String, Object>> orphanedFiles =
                    fileStorageService.findTempFilesOlderThan(2);

            if (orphanedFiles.isEmpty()) {
                log.info("Cloudinary cleanup: no orphaned temp images found");
                return;
            }

            log.info("Cloudinary cleanup: found {} orphaned temp image(s) to delete",
                    orphanedFiles.size());

            int deleted = 0;
            int failed  = 0;

            for (Map<String, Object> resource : orphanedFiles) {
                String publicId = (String) resource.get("public_id");
                if (publicId == null) continue;

                try {
                    fileStorageService.deleteFile(publicId);
                    deleted++;
                } catch (Exception e) {
                    log.warn("Cleanup could not delete {}: {}", publicId, e.getMessage());
                    failed++;
                }
            }

            log.info("Cloudinary cleanup complete — deleted: {}, failed: {}", deleted, failed);

        } catch (Exception e) {
            log.error("Cloudinary cleanup job error: {}", e.getMessage());
        }
    }
}