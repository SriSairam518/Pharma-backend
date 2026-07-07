package com.sairam.pharma.service;

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