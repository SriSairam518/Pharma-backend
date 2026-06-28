package com.sairam.pharma.service;

// ================================================================
// FileStorageService.java  —  CLOUDINARY with temp/permanent tagging
//
// TWO-PHASE UPLOAD STRATEGY:
//
// PHASE 1 — Upload (temporary):
//   User uploads image → stored in Cloudinary with tag "temp"
//   Returns the URL immediately so OCR can start
//   If bill save fails or user cancels → image stays tagged "temp"
//
// PHASE 2 — Confirm (permanent):
//   Bill/Payment saved successfully → we call confirmFile(url)
//   This removes the "temp" tag → image becomes permanent
//
// CLEANUP (automated):
//   CloudinaryCleanupService runs every hour
//   Finds all Cloudinary images still tagged "temp" AND older than 2 hours
//   Deletes them automatically — no manual intervention needed
//
// WHY TAGS INSTEAD OF MOVING FILES?
// Cloudinary has no "move" operation. Tags are the standard Cloudinary
// way to mark files as temporary vs permanent. The cleanup service
// uses Cloudinary's Admin API to search by tag + created_at date.
// ================================================================

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    public static final String TEMP_TAG = "pharma_temp";

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true
        ));
        log.info("Cloudinary initialized — cloud");
    }

    public String storeFile(MultipartFile file, String subFolder) {
        try {
            String publicId = "pharma/" + subFolder + "/" + UUID.randomUUID();

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id",     publicId,
                            "resource_type", "auto",
                            "tags",          List.of(TEMP_TAG)   // ← marked as temp
                    )
            );

            String url = (String) result.get("secure_url");
            log.info("Uploaded temp file to Cloudinary [{}]: {}", subFolder, publicId);
            return url;

        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public void confirmFile(String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) return;
        try {
            String publicId = extractPublicId(cloudinaryUrl);
            if (publicId == null) {
                log.warn("Could not extract publicId from URL");
                return;
            }

            cloudinary.uploader().explicit(publicId, ObjectUtils.asMap(
                    "type",          "upload",
                    "resource_type", "image",
                    "tags",          ""
            ));
            log.info("Confirmed file (temp tag removed): {}", publicId);

        } catch (Exception e) {
            log.warn("Could not confirm file {}", e.getMessage());
        }
    }

    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", "image"));
            log.info("Deleted orphaned temp file: {}", publicId);
        } catch (Exception e) {
            log.warn("Could not delete file {}: {}", publicId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findTempFilesOlderThan(int hours) {
        try {
            long cutoffTimestamp = System.currentTimeMillis() / 1000 - (hours * 3600L);

            Map<String, Object> result = cloudinary.api()
                    .resourcesByTag(TEMP_TAG, ObjectUtils.asMap(
                            "max_results",   100,
                            "resource_type", "image"
                    ));

            List<Map<String, Object>> resources =
                    (List<Map<String, Object>>) result.get("resources");

            if (resources == null) return List.of();

            return resources.stream()
                    .filter(r -> {
                        Object createdAt = r.get("created_at");
                        if (createdAt == null) return false;
                        try {
                            long created = java.time.Instant.parse(createdAt.toString())
                                    .getEpochSecond();
                            return created < cutoffTimestamp;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .toList();

        } catch (Exception e) {
            log.error("Error searching temp files: {}", e.getMessage());
            return List.of();
        }
    }

    public String extractPublicId(String url) {
        if (url == null) return null;
        try {
            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;

            String afterUpload = parts[1];

            String withoutVersion = afterUpload.replaceFirst("^v\\d+/", "");

            int dotIndex = withoutVersion.lastIndexOf('.');
            return dotIndex > 0 ? withoutVersion.substring(0, dotIndex) : withoutVersion;

        } catch (Exception e) {
            log.warn("Failed to extract publicId from URL: {}", url);
            return null;
        }
    }

    public Cloudinary getCloudinary() {

        return cloudinary;
    }
}