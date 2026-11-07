
package com.aashir.notificationservice.sender;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        emailSender = new EmailSender(mailSender);
    }

    @Test
    void sendCreatesAndSendsCorrectHtmlEmail() throws Exception {
        MimeMessage message = new MimeMessage(
                Session.getInstance(new Properties())
        );

        when(mailSender.createMimeMessage()).thenReturn(message);

        String htmlBody = "<html><body><h1>Welcome!</h1></body></html>";

        emailSender.send(
                "aashir@example.com",
                "Welcome to Forma",
                htmlBody
        );

        message.saveChanges();

        Address[] recipients = message.getRecipients(Message.RecipientType.TO);

        assertNotNull(recipients);
        assertEquals(1, recipients.length);
        assertEquals("aashir@example.com", recipients[0].toString());

        assertEquals("Welcome to Forma", message.getSubject());
        assertEquals(htmlBody, findHtmlContent(message));

        verify(mailSender).send(message);
    }

    @Test
    void sendCreatesMimeMessageWithUtf8Encoding() throws Exception {
        MimeMessage message = new MimeMessage(
                Session.getInstance(new Properties())
        );

        when(mailSender.createMimeMessage()).thenReturn(message);

        emailSender.send(
                "aashir@example.com",
                "Order Confirmed",
                "<h1>Order confirmed</h1>"
        );

        message.saveChanges();

        String htmlContentType = findHtmlContentType(message);

        assertNotNull(htmlContentType);
        assertTrue(
                htmlContentType.toLowerCase().contains("charset=utf-8"),
                "HTML content should use UTF-8 encoding"
        );

        verify(mailSender).send(message);
    }

    @Test
    void messagingExceptionIsWrappedInIllegalStateException() throws Exception {
        MimeMessage message = new MimeMessage(
                Session.getInstance(new Properties())
        ) {
            @Override
            public void setRecipients(
                    Message.RecipientType type,
                    Address[] addresses
            ) throws MessagingException {
                throw new MessagingException("Invalid recipient");
            }
        };

        when(mailSender.createMimeMessage()).thenReturn(message);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> emailSender.send(
                        "invalid-email",
                        "Test Subject",
                        "<h1>Test</h1>"
                )
        );

        assertEquals(
                "Failed to send email to invalid-email",
                exception.getMessage()
        );

        assertInstanceOf(MessagingException.class, exception.getCause());

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private String findHtmlContent(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();

            for (int i = 0; i < multipart.getCount(); i++) {
                String content = findHtmlContent(multipart.getBodyPart(i));

                if (content != null) {
                    return content;
                }
            }
        }

        return null;
    }

    private String findHtmlContentType(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return part.getContentType();
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();

            for (int i = 0; i < multipart.getCount(); i++) {
                String contentType = findHtmlContentType(
                        multipart.getBodyPart(i)
                );

                if (contentType != null) {
                    return contentType;
                }
            }
        }

        return null;
    }
}
