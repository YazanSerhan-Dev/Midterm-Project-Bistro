package Client;

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


    public static void configure(String host, int port) {
        ClientSession.host = host;
        ClientSession.port = port;
    }

    public static BistroClient getClient() {
        return client;
    }
    
    public static void setRole(String r) { role = r; }
    public static String getRole() { return role; }

    public static void setUsername(String u) { username = u; }
    public static String getUsername() { return username; }
    
    public static String getMemberCode() { return memberCode; }
    public static void setMemberCode(String code) { memberCode = code; }
  

    public static String getGuestEmail() {
        return guestEmail;
    }

    public static void setGuestEmail(String email) {
        guestEmail = email;
    }

    public static String getGuestPhone() {
        return guestPhone;
    }

    public static void setGuestPhone(String phone) {
        guestPhone = phone;
    }



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

    public static void bindUI(ClientUI ui) {
        if (client != null) {
            client.setUI(ui);
        }
    }

    public static void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.closeConnection();
            }
        } catch (Exception ignored) {}
    }
    
    public static void clearGuestIdentity() {
        guestEmail = null;
        guestPhone = null;
    }
    
    public static void clearAllIdentity() {
        role = "CUSTOMER";
        username = "";
        guestEmail = null;
        guestPhone = null;
        memberCode = null;
    }


}

