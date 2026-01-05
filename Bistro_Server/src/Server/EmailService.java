package Server;

import java.io.FileInputStream;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailService {

    // ✅ Option 2: absolute path on YOUR PC
    // Change this path if you saved the file somewhere else.
    private static final String CONFIG_PATH = "C:\\Users\\yazan\\Downloads\\email.properties";

    // Keys in email.properties:
    // smtp.host=smtp.gmail.com
    // smtp.port=587
    // email.from=your_project_gmail@gmail.com
    // email.password=YOUR_APP_PASSWORD
    private static Properties loadCfg() {
        Properties cfg = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_PATH)) {
            cfg.load(in);
            return cfg;
        } catch (Exception e) {
            System.out.println("[EMAIL] Failed to load config from: " + CONFIG_PATH + " | " + e.getMessage());
            return null;
        }
    }

    /**
     * Internal helper used by ALL email types (confirmation / reminder / etc.)
     */
    private static void sendEmail(String toEmail, String subject, String body) {
        Properties cfg = loadCfg();
        if (cfg == null) return;

        String smtpHost = cfg.getProperty("smtp.host");
        String smtpPort = cfg.getProperty("smtp.port");
        String fromEmail = cfg.getProperty("email.from");
        String appPassword = cfg.getProperty("email.password");

        if (smtpHost == null || smtpHost.isBlank()
                || smtpPort == null || smtpPort.isBlank()
                || fromEmail == null || fromEmail.isBlank()
                || appPassword == null || appPassword.isBlank()) {

            System.out.println("[EMAIL] email.properties missing required keys. "
                    + "Expected: smtp.host, smtp.port, email.from, email.password");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, appPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail, "Bistro System"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);

            System.out.println("[EMAIL] Sent to: " + toEmail + " | Subject: " + subject);

        } catch (Exception e) {
            System.out.println("[EMAIL] Failed to send to " + toEmail + " : " + e.getMessage());
        }
    }

    public static void sendReservationConfirmation(String toEmail, String confirmationCode) {
        String subject = "Bistro Reservation Confirmation";
        String body = "Your reservation has been created successfully!\n\n"
                + "Confirmation Code: " + confirmationCode + "\n\n"
                + "Please keep this code to manage your reservation.\n";

        sendEmail(toEmail, subject, body);
    }

    /**
     * 2-hours reminder email.
     */
    public static void sendReservationReminder(String toEmail, String confirmationCode, String timeStr) {
        String subject = "Bistro Reminder - Reservation in 2 hours";
        String body =
                "Hello,\n\n" +
                "This is a reminder that your reservation is in 2 hours.\n\n" +
                "Time: " + timeStr + "\n" +
                "Confirmation code: " + confirmationCode + "\n\n" +
                "See you soon,\n" +
                "Bistro System";

        sendEmail(toEmail, subject, body);
    }
    
    public static void sendWaitingTableReady(String toEmail, String waitingCode) {
        String subject = "Bistro - Your table is ready!";
        String body =
                "Hello,\n\n" +
                "A table is now available for you.\n\n" +
                "Please check in within 15 minutes using this code:\n" +
                waitingCode + "\n\n" +
                "See you soon,\n" +
                "Bistro System";

        sendEmail(toEmail, subject, body);
    }


}

