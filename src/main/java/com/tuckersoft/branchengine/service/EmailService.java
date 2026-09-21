package com.tuckersoft.branchengine.service;

public interface EmailService {

    /**
     * Plain-text email. Throws MailException (unchecked) on SMTP failure --
     * callers (typically an @Async listener) decide how to record that,
     * per whatever the spec's error-handling rules are. This method never
     * swallows the exception itself, since "was it sent or not" is usually
     * something the caller must persist.
     */
    void sendSimpleMessage(String to, String subject, String text);
}
