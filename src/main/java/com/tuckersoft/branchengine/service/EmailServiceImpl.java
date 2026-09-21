package com.tuckersoft.branchengine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        // Some SMTP servers reject messages with no "from" -- if yours does,
        // set spring.mail.username as the from address explicitly:
        // message.setFrom("<spring.mail.username value>");
        mailSender.send(message);
    }
}
