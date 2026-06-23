package com.sairam.pharma.service;

// ================================================================
// EmailService.java
//
// WHY @Autowired(required = false)?
// JavaMailSender requires MAIL_USERNAME + MAIL_PASSWORD env vars
// to be set. In local development you often don't have these
// configured. Without this, the whole app fails to start just
// because mail isn't set up — even though you might only want to
// test billing features, not password reset.
//
// With required=false:
//   - Mail env vars SET   → JavaMailSender is created, emails work
//   - Mail env vars MISSING → JavaMailSender is null, app still
//     starts, but calling forgotPassword() returns a clear error
//     instead of crashing at startup
// ================================================================

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
public class EmailService {

    // required = false → app starts even if mail is not configured
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {

        // Guard — gives a clear error if mail isn't configured
        // instead of a NullPointerException deep in the stack
        if (mailSender == null) {
            log.error("Mail is not configured. Set MAIL_USERNAME and MAIL_PASSWORD env vars.");
            throw new RuntimeException(
                    "Email service is not configured. Please contact the administrator."
            );
        }

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String subject   = "PharmaMS — Password Reset Request";

        String htmlBody = """
            <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:20px;">
              <div style="background:#1a7a5e;padding:20px;border-radius:8px 8px 0 0;">
                <h1 style="color:white;margin:0;font-size:20px;">PharmaMS</h1>
                <p style="color:#a8f0d8;margin:4px 0 0;font-size:13px;">Medical Shop Management</p>
              </div>
              <div style="background:#f9fafb;padding:24px;border:1px solid #e5e7eb;border-radius:0 0 8px 8px;">
                <h2 style="color:#111827;font-size:18px;margin:0 0 12px;">Reset your password</h2>
                <p style="color:#374151;font-size:14px;line-height:1.6;">
                  We received a request to reset the password for your PharmaMS account.
                  Click the button below to set a new password.
                </p>
                <div style="text-align:center;margin:24px 0;">
                  <a href="%s" style="background:#1a7a5e;color:white;padding:12px 28px;
                    border-radius:6px;text-decoration:none;font-size:15px;font-weight:600;
                    display:inline-block;">Reset Password</a>
                </div>
                <p style="color:#6b7280;font-size:13px;line-height:1.6;">
                  This link expires in <strong>15 minutes</strong>.
                  If you didn't request a reset, ignore this email — your password won't change.
                </p>
                <hr style="border:none;border-top:1px solid #e5e7eb;margin:20px 0;">
                <p style="color:#9ca3af;font-size:12px;margin:0;">
                  If the button doesn't work, paste this into your browser:<br>
                  <a href="%s" style="color:#1a7a5e;word-break:break-all;">%s</a>
                </p>
              </div>
            </div>
            """.formatted(resetLink, resetLink, resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Password reset email sent successfully");
        } catch (MessagingException e) {
            log.error("Failed to send reset email: {}", e.getMessage());
            throw new RuntimeException("Could not send reset email. Please try again.");
        }
    }
}