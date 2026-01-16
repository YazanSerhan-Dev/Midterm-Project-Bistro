package Client;
/**
 * Manages the client-side session state for the Bistro application.
 * <p>
 * This class holds shared static data related to the current client connection,
 * user role, identity (subscriber or guest), and server configuration.
 * </p>
 * <p>
 * Acts as a central access point for a single {@link BistroClient} instance
 * and allows switching the active UI receiver without reconnecting.
 * </p>
 */

public class ClientSession {

    private static BistroClient client;
    private static String host = "localhost";
    private static int port = 5555;
    private static String role = "CUSTOMER";
    private static String username = "";
    private static String memberCode; // subscriber primary key (member_code)
 // ===== Guest (Customer) identity (for "My Reservations") =====
    private static String guestEmail;
    private static String guestPhone;

    /**
     * Configures the server connection parameters.
     *
     * @param host server hostname or IP address
     * @param port server port number
     */

    public static void configure(String host, int port) {
        ClientSession.host = host;
        ClientSession.port = port;
    }
    /**
     * Returns the shared client instance.
     *
     * @return the current {@link BistroClient}, or null if not yet created
     */

    public static BistroClient getClient() {
        return client;
    }
    /**
     * Sets the current user role.
     *
     * @param r user role (e.g., SUBSCRIBER or CUSTOMER)
     */

    public static void setRole(String r) { role = r; }
    /**
     * Returns the current user role.
     *
     * @return user role string
     */

    public static String getRole() { return role; }
    /**
     * Sets the username of the current subscriber user.
     *
     * @param u subscriber username
     */

    public static void setUsername(String u) { username = u; }
    /**
     * Returns the username of the current subscriber user.
     *
     * @return username string
     */

    public static String getUsername() { return username; }
    /**
     * Returns the subscriber member code.
     *
     * @return member code, or null if not set
     */

    public static String getMemberCode() { return memberCode; }
    /**
     * Sets the subscriber member code.
     *
     * @param code subscriber primary key (member_code)
     */

    public static void setMemberCode(String code) { memberCode = code; }
  
    /**
     * Returns the guest customer's email address.
     *
     * @return guest email, or null if not set
     */

    public static String getGuestEmail() {
        return guestEmail;
    }
    /**
     * Sets the guest customer's email address.
     *
     * @param email guest email
     */

    public static void setGuestEmail(String email) {
        guestEmail = email;
    }
    /**
     * Returns the guest customer's phone number.
     *
     * @return guest phone number, or null if not set
     */

    public static String getGuestPhone() {
        return guestPhone;
    }
    /**
     * Sets the guest customer's phone number.
     *
     * @param phone guest phone number
     */

    public static void setGuestPhone(String phone) {
        guestPhone = phone;
    }


    /**
     * Creates or reuses a single client connection and binds it to the given UI.
     * <p>
     * If a client already exists, the UI receiver is updated instead of creating
     * a new connection.
     * </p>
     *
     * @param ui the UI instance to receive client callbacks
     * @throws Exception if the connection cannot be opened
     */

    // Connect once (or reuse if already connected)
    public static void connect(ClientUI ui) throws Exception {
        if (client == null) {
            client = new BistroClient(host, port, ui);
            client.openConnection();
            return;
        }

        // reuse existing client, just swap who receives callbacks
        client.setUI(ui);

        if (!client.isConnected()) {
            client.openConnection();
        }
    }
    /**
     * Binds a new UI receiver to the existing client connection.
     *
     * @param ui the UI instance to receive callbacks
     */

    public static void bindUI(ClientUI ui) {
        if (client != null) {
            client.setUI(ui);
        }
    }
    /**
     * Disconnects the client from the server if a connection exists.
     */

    public static void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.closeConnection();
            }
        } catch (Exception ignored) {}
    }
    /**
     * Clears stored guest customer identity information (email and phone).
     */

    public static void clearGuestIdentity() {
        guestEmail = null;
        guestPhone = null;
    }
    /**
     * Clears all stored session identity information, including role,
     * subscriber data, and guest contact details.
     */

    public static void clearAllIdentity() {
        role = "CUSTOMER";
        username = "";
        guestEmail = null;
        guestPhone = null;
        memberCode = null;
    }


}

