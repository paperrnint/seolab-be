package com.example.seolab.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

	private final JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String fromEmail;

	@Async
	public void sendVerificationEmail(String toEmail, String verificationCode) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setFrom(fromEmail);
			helper.setTo(toEmail);
			helper.setSubject("도토리 - 이메일 인증 코드");
			helper.setText(createHtmlEmailContent(verificationCode), true); // HTML 모드

			mailSender.send(message);
			log.info("HTML verification email sent to: {}", toEmail);
		} catch (Exception e) {
			log.error("Failed to send verification email to: {}", toEmail, e);
			throw new RuntimeException("이메일 발송에 실패했습니다.", e);
		}
	}

	private String createHtmlEmailContent(String verificationCode) {
		return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>도토리 이메일 인증</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                
                <div style="background: #ffffff; padding: 40px; border-radius: 0 0 10px 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                    <h2 style="color: #333; text-align: center; margin-bottom: 30px;">이메일 인증 코드</h2>
                    
                    <div style="background: #f8f9fa; border: 2px dashed #667eea; border-radius: 8px; padding: 30px; text-align: center; margin: 30px 0;">
                        <p style="font-size: 14px; color: #666; margin: 0 0 10px 0;">인증 코드</p>
                        <h1 style="font-size: 36px; font-weight: bold; color: #667eea; margin: 0; letter-spacing: 5px; font-family: 'Courier New', monospace;">%s</h1>
                    </div>
                    
                    <div style="background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 6px; padding: 15px; margin: 25px 0;">
                        <p style="margin: 0; font-size: 14px; color: #856404;">
                            <strong>⚠️</strong> 인증 코드는 <strong>5분 후에 만료</strong>됩니다.
                        </p>
                    </div>
                    
                    <div style="margin-top: 40px; padding-top: 30px; border-top: 1px solid #eee; text-align: center;">
                        <p style="color: #666; font-size: 14px; margin: 0;">
                            본 이메일은 도토리 회원가입을 위해 발송되었습니다.<br>
                            인증시간이 만료되었을 경우, 인증번호 재발송을 진행해 주시기 바랍니다.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, verificationCode);
	}
}
