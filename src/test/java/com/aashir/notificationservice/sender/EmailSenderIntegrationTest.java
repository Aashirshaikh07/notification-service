package com.aashir.notificationservice.sender;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.*;

class EmailSenderIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail =
            new GreenMailExtension(
                    new ServerSetup(3025, "localhost", "smtp")
            );

    @Test
    void shouldSendHtmlEmailSuccessfullyThroughSmtp() throws Exception {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(3025);
        mailSender.setProtocol("smtp");

        EmailSender emailSender = new EmailSender(mailSender);

        String recipient = "customer@example.com";
        String subject = "Order Confirmation";
        String htmlBody = """
                <html>
                    <body>
                        <h1>Order Confirmed!</h1>
                        <p>Your order has been placed successfully.</p>
                    </body>
                </html>
                """;

        emailSender.send(recipient, subject, htmlBody);

        boolean emailReceived = greenMail.waitForIncomingEmail(5000, 1);

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();

        assertTrue(
                emailReceived,
                "SMTP server should receive one email. Actual received: "
                        + receivedMessages.length
        );

        assertEquals(1, receivedMessages.length);

        MimeMessage receivedMessage = receivedMessages[0];

        assertEquals(
                recipient,
                receivedMessage.getAllRecipients()[0].toString()
        );

        assertEquals(subject, receivedMessage.getSubject());

        String receivedBody = findHtmlContent(receivedMessage);

        assertNotNull(receivedBody, "No text/html part found in the message");
        assertTrue(receivedBody.contains("Order Confirmed!"));
        assertTrue(receivedBody.contains("successfully"));
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
}