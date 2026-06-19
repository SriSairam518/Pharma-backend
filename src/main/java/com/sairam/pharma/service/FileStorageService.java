package com.sairam.pharma.service;

// ================================================================
// FileStorageService.java  —  SERVICE
//
// WHAT IS THIS?
// Handles saving uploaded files (bill images, payment proofs)
// to the server's local disk, inside an "uploads" folder.
//
// WHY NOT STORE IMAGES IN THE DATABASE?
// Databases are optimized for structured data (text, numbers),
// not large binary files. Storing images as files on disk (or
// later, cloud storage like S3/Cloudinary) and just saving the
// FILE PATH in the database is the standard approach.
//
// FOLDER STRUCTURE CREATED:
//   uploads/
//     bills/      ← bill scan images
//     payments/   ← payment proof images
//
// HOW FILES ARE SERVED BACK:
// We configure a static resource handler (WebConfig.java) so
// files in "uploads/" are accessible at http://localhost:8080/uploads/...
// ================================================================

// ================================================================
// FileStorageService.java  —  CLOUDINARY IMAGE STORAGE
//
// WHY CLOUDINARY INSTEAD OF LOCAL DISK?
// Render (our backend host) has NO persistent disk on the free tier.
// Any files saved to disk disappear when the server restarts
// (which happens after 15 min of inactivity on the free tier).
//
// Cloudinary stores files permanently in the cloud.
// We upload the file → get back a permanent URL → store that URL
// in the database. Even if our server restarts, the image URL
// always works because it points to Cloudinary, not our server.
//
// FREE TIER: 25GB storage + 25GB bandwidth/month — more than
// enough for a medical shop (each bill scan ≈ 500KB–2MB).
// ================================================================

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
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

    private Cloudinary cloudinary;

    // @PostConstruct runs once after Spring creates this bean
    // and all @Value fields are injected — safe to use them here
    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true    // always use HTTPS URLs
        ));
        log.info("Cloudinary initialized for cloud: {}", cloudName);
    }

    // ================================================================
    // UPLOAD FILE TO CLOUDINARY
    //
    // subFolder = "bills" or "payments"
    // Returns: permanent HTTPS URL (e.g. https://res.cloudinary.com/...)
    //
    // This URL is stored in the DB. The frontend uses it directly
    // to display the image — no backend proxy needed.
    // ================================================================
    public String storeFile(MultipartFile file, String subFolder) {
        try {
            // Generate a unique public ID for this file in Cloudinary
            // Format: pharma/bills/uuid  or  pharma/payments/uuid
            String publicId = "pharma/" + subFolder + "/" + UUID.randomUUID();

            // Upload to Cloudinary
            // "folder" organises files in the Cloudinary dashboard
            // "resource_type" = auto detects image vs PDF
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id",     publicId,
                            "resource_type", "auto",   // handles images AND PDFs
                            "folder",        "pharma/" + subFolder
                    )
            );

            // Cloudinary returns the secure_url in the result map
            String url = (String) result.get("secure_url");
            log.info("File uploaded to Cloudinary: {}", url);
            return url;

        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }
}