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
/**
 * Utility service for sending system emails (reservation confirmations, reminders, waiting-list updates).
 * <p>
 * Reads SMTP configuration from an external {@code email.properties} file and sends plain-text emails
 * using Jakarta Mail (SMTP + STARTTLS).
 * <p>
 * Notes:
 * <ul>
 *   <li>This class is server-side only.</li>
 *   <li>Configuration path is currently absolute and should match the server machine.</li>
 * </ul>
 */
public class EmailService {

    // ✅ Option 2: absolute path on YOUR PC
    // Change this path if you saved the file somewhere else.
    private static final String CONFIG_PATH = "C:\\Users\\Lenovo2024\\Downloads\\email.properties";

    // Keys in email.properties:
    // smtp.host=smtp.gmail.com
    // smtp.port=587
    // email.from=your_project_gmail@gmail.com
    // email.password=YOUR_APP_PASSWORD
    /**
     * Loads SMTP/email configuration from {@link #CONFIG_PATH}.
     *
     * @return loaded {@link Properties} or {@code null} if the file cannot be loaded.
     */
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
     * Low-level internal sender used by all public email methods.
     * <p>
     * Validates required properties, opens an SMTP session (auth + STARTTLS),
     * builds a plain-text email message and sends it.
     *
     * @param toEmail recipient email address
     * @param subject email subject
     * @param body plain-text body
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
    /**
     * Sends a reservation confirmation email containing a confirmation code.
     *
     * @param toEmail recipient email
     * @param confirmationCode reservation confirmation code
     */
    public static void sendReservationConfirmation(String toEmail, String confirmationCode) {
        String subject = "Bistro Reservation Confirmation";
        String body = "Your reservation has been created successfully!\n\n"
                + "Confirmation Code: " + confirmationCode + "\n\n"
                + "Please keep this code to manage your reservation.\n";

        sendEmail(toEmail, subject, body);
    }

    /**
     * Sends a reminder email 2 hours before a reservation time.
     *
     * @param toEmail recipient email
     * @param confirmationCode reservation confirmation code
     * @param timeStr reservation time as display string (e.g. "18:30")
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

    /**
     * Sends an email notifying a waiting-list guest that a table is ready.
     * The guest is expected to check-in within the hold window (e.g. 15 minutes).
     *
     * @param toEmail recipient email
     * @param waitingCode waiting-list confirmation code
     */
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

    /**
     * Sends a payment reminder (bill reminder) email with the confirmation code.
     *
     * @param toEmail recipient email
     * @param confirmationCode code used to identify the visit/payment
     */
    public static void sendBillReminder(String toEmail, String confirmationCode) {
        String subject = "Reminder: Please complete your payment";
        String body = """
            Hi,
            
            This is a friendly reminder that your visit has reached the 2-hour limit.
            Please proceed to payment using your confirmation code:

            Confirmation Code: %s

            Thank you,
            Bistro
            """.formatted(confirmationCode);

        sendEmail(toEmail, subject, body); // use your existing internal sendEmail()
    }
    /**
     * Sends an email notifying a reservation guest that the assigned table is ready.
     *
     * @param toEmail recipient email
     * @param tableId assigned table identifier (e.g. "T12")
     */
    public static void sendReservationTableReady(String toEmail, String tableId) {
        if (toEmail == null || toEmail.isBlank()) return;

        String subject = "Bistro: Your table is ready";
        String body =
                "Hello,\n\n" +
                "Your table is ready: " + tableId + ".\n" +
                "Please go to the table now and scan your confirmation code again at the terminal within 15 minutes to confirm.\n\n" +
                "Thank you,\n" +
                "Bistro";

        try {
            // Use your existing low-level method here.
            // Replace 'sendEmail' with whatever you already have (sendMail / send / sendTo...).
            sendEmail(toEmail, subject, body);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Temporary SMS stub for testing/logging (no real SMS integration).
     *
     * @param phone phone number
     * @param msg message text
     */
    public static void smsStub(String phone, String msg) {
        if (phone == null || phone.isBlank()) return;
        System.out.println("[SMS] phone=" + phone + " | " + msg);
    }


}

