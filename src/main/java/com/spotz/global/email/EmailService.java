package com.spotz.global.email;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[Spotz] 이메일 인증 코드");
        message.setText("인증 코드: " + code + "\n\n5분 내에 입력해주세요.");
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[Spotz] 비밀번호 재설정 인증 코드");
        message.setText("비밀번호 재설정 인증 코드: " + code + "\n\n5분 내에 입력해주세요.\n본인이 요청하지 않았다면 이 메일을 무시하세요.");
        mailSender.send(message);
    }
}
