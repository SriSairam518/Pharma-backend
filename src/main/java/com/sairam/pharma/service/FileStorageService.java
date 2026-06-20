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

    // Tag used to mark images as "not yet confirmed by a saved bill/payment"
    // The cleanup job will delete any image still having this tag after 2 hours
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
        log.info("Cloudinary initialized — cloud: {}", cloudName);
    }

    // ================================================================
    // PHASE 1 — Upload with TEMP tag
    // Called when user picks a file in the browser.
    // Image is stored BUT tagged as temporary.
    // Returns the permanent URL (the URL itself never changes —
    // only the tag changes when confirmed).
    // ================================================================
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

    // ================================================================
    // PHASE 2 — Confirm (make permanent)
    // Called after a bill or payment is successfully saved to DB.
    // Removes the TEMP_TAG → cleanup job will no longer touch this image.
    //
    // HOW WE GET publicId FROM URL:
    // Cloudinary URL format:
    //   https://res.cloudinary.com/{cloud}/image/upload/v{ver}/{publicId}.{ext}
    // We extract everything after "upload/v{version}/" as the public ID.
    // ================================================================
    public void confirmFile(String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) return;
        try {
            String publicId = extractPublicId(cloudinaryUrl);
            if (publicId == null) {
                log.warn("Could not extract publicId from URL: {}", cloudinaryUrl);
                return;
            }

            // Use explicit() to update the resource and clear all tags.
            // removeTag() API was removed in newer Cloudinary SDK versions.
            // Setting tags="" removes all tags including TEMP_TAG.
            cloudinary.uploader().explicit(publicId, ObjectUtils.asMap(
                    "type",          "upload",
                    "resource_type", "image",
                    "tags",          ""
            ));
            log.info("Confirmed file (temp tag removed): {}", publicId);

        } catch (Exception e) {
            // Non-fatal — image is already saved, just couldn't remove tag
            // The cleanup job will skip images that are referenced in DB (safe)
            log.warn("Could not confirm file {}: {}", cloudinaryUrl, e.getMessage());
        }
    }

    // ================================================================
    // DELETE — used by cleanup job to remove orphaned temp images
    // ================================================================
    public void deleteFile(String publicId) {
        try {
            // destroy() is part of the Upload API where "auto" IS valid,
            // but we use "image" explicitly for consistency since all
            // our files (bill scans, payment proofs) are stored as images
            cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", "image"));
            log.info("Deleted orphaned temp file: {}", publicId);
        } catch (Exception e) {
            log.warn("Could not delete file {}: {}", publicId, e.getMessage());
        }
    }

    // ================================================================
    // SEARCH temp files older than given hours — used by cleanup job
    // ================================================================
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findTempFilesOlderThan(int hours) {
        try {
            // Cloudinary Admin API search — find all resources with our temp tag
            // that were created before (now - hours)
            //
            // IMPORTANT: "auto" is only valid for the UPLOAD api (it lets
            // Cloudinary detect image/video/raw automatically). The Admin
            // API used here for SEARCHING does not accept "auto" — it only
            // accepts a specific type: "image", "video", or "raw".
            // All our bill scans and payment proofs are uploaded as images
            // (Cloudinary auto-converts PDFs to images too), so "image" is correct.
            long cutoffTimestamp = System.currentTimeMillis() / 1000 - (hours * 3600L);

            Map<String, Object> result = cloudinary.api()
                    .resourcesByTag(TEMP_TAG, ObjectUtils.asMap(
                            "max_results",   100,
                            "resource_type", "image"
                    ));

            List<Map<String, Object>> resources =
                    (List<Map<String, Object>>) result.get("resources");

            if (resources == null) return List.of();

            // Filter to only those older than the cutoff
            return resources.stream()
                    .filter(r -> {
                        Object createdAt = r.get("created_at");
                        if (createdAt == null) return false;
                        // created_at is ISO string like "2024-06-01T10:00:00Z"
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

    // ================================================================
    // HELPERS
    // ================================================================

    // Extract Cloudinary publicId from a secure URL
    // Input:  https://res.cloudinary.com/mycloud/image/upload/v1234567/pharma/bills/uuid.jpg
    // Output: pharma/bills/uuid
    public String extractPublicId(String url) {
        if (url == null) return null;
        try {
            // Split on "/upload/" and take the part after it
            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;

            String afterUpload = parts[1]; // e.g. "v1234567/pharma/bills/uuid.jpg"

            // Remove version prefix (v followed by digits and slash)
            String withoutVersion = afterUpload.replaceFirst("^v\\d+/", "");

            // Remove file extension
            int dotIndex = withoutVersion.lastIndexOf('.');
            return dotIndex > 0 ? withoutVersion.substring(0, dotIndex) : withoutVersion;

        } catch (Exception e) {
            log.warn("Failed to extract publicId from URL: {}", url);
            return null;
        }
    }

    // Expose cloudinary instance for cleanup service
    public Cloudinary getCloudinary() {
        return cloudinary;
    }
}