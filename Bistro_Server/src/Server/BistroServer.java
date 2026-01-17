package Server;

import java.io.IOException;
import java.sql.Date; // From HEAD
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.sql.CommonDataSource;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

import common.Envelope;
import common.KryoMessage;
import common.KryoUtil;
import common.OpCode;

// Imports from HEAD (Registration)
import common.dto.RegistrationDTO;
import common.dto.ReservationDTO;
import common.dto.SubscriberDTO;
import common.dto.TerminalActiveItemDTO;
import common.dto.TerminalValidateResponseDTO;
import common.dto.WaitingListDTO;
// Imports from MAIN (Reservation & Login)
import common.dto.LoginResponseDTO;
import common.dto.MakeReservationRequestDTO;
import common.dto.MakeReservationResponseDTO;
import common.dto.CurrentDinersDTO; // Make sure to import this

import DataBase.dao.VisitDAO;
import common.dto.ProfileDTO;
import DataBase.Reservation;
import DataBase.dao.BillDAO;
import DataBase.dao.OpeningHoursDAO;
import DataBase.dao.ReservationDAO;
import DataBase.dao.SubscriberDAO;
import DataBase.dao.UserActivityDAO;
import DataBase.dao.VisitDAO;
import DataBase.dao.WaitingListDAO;
import DataBase.dao.RestaurantTableDAO; // From MAIN
/**
 * Main OCSF server of the Bistro system.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Starts/stops server and background jobs.</li>
 *   <li>Receives client requests (as {@link Envelope}) and routes them by {@link OpCode}.</li>
 *   <li>Performs business validations and calls DAO layer.</li>
 *   <li>Sends responses back using {@link KryoMessage} serialization.</li>
 * </ul>
 * </p>
 */
public class BistroServer extends AbstractServer {
	 /** UI controller for the server window (may be null when running headless). */
    private final ServerController controller;
    /** DAO for reservation operations (non-static methods exist here). */
    private final ReservationDAO reservationDAO = new ReservationDAO();
    /** DAO for subscriber operations (kept for future/consistency). */
    private final SubscriberDAO subscriberDAO = new SubscriberDAO();
    /**
     * Creates a new server instance.
     *
     * @param port       listening port
     * @param controller JavaFX controller used to update UI/logs (can be null)
     */
    public BistroServer(int port, ServerController controller) {
        super(port);
        this.controller = controller;
    }
    /**
     * Writes a log line either to the UI (if available) or to console.
     *
     * @param text message to log
     */
    private void log(String text) {
        if (controller != null) controller.appendLogFromServer(text);
        else System.out.println(text);
    }
    /**
     * Extracts host name from client connection.
     *
     * @param c client connection
     * @return host name or "unknown" if missing
     */
    private String host(ConnectionToClient c) {
        return (c.getInetAddress() != null) ? c.getInetAddress().getHostName() : "unknown";
    }
    /**
     * Extracts IP address from client connection.
     *
     * @param c client connection
     * @return ip address or "unknown" if missing
     */
    private String ip(ConnectionToClient c) {
        return (c.getInetAddress() != null) ? c.getInetAddress().getHostAddress() : "unknown";
    }
    /**
     * Called by OCSF when server starts listening.
     * Starts background jobs and updates UI.
     */
    @Override
    protected void serverStarted() {
        log("Server started on port " + getPort());
        if (controller != null) controller.onServerStarted(getPort());
        BackgroundJobs.start();
    }
    /**
     * Called by OCSF when server is stopped.
     * Stops background jobs and updates UI.
     */
    @Override
    protected void serverStopped() {
        log("Server stopped.");
        if (controller != null) controller.onServerStopped();
        BackgroundJobs.stop();
    }
    /**
     * Called when a client connects.
     * Stores host/ip as client info and updates UI.
     *
     * @param client client that connected
     */
    @Override
    protected void clientConnected(ConnectionToClient client) {
        super.clientConnected(client);
        String host = host(client);
        String ip = ip(client);
        log("Client connected: " + host + " (" + ip + ")");
        client.setInfo("host", host);
        client.setInfo("ip", ip);
        if (controller != null) controller.onClientConnected(host, ip);
    }
    /**
     * Called when a client disconnects normally.
     *
     * @param client client that disconnected
     */
    @Override
    protected void clientDisconnected(ConnectionToClient client) {
        super.clientDisconnected(client);
        String host = (String) client.getInfo("host");
        String ip = (String) client.getInfo("ip");
        if (host == null) host = host(client);
        if (ip == null) ip = ip(client);
        log("Client disconnected: " + host + " (" + ip + ")");
        if (controller != null) controller.onClientDisconnected(host, ip);
    }

    /**
     * Called when client disconnects due to an exception.
     *
     * @param client    client connection
     * @param exception thrown error
     */
    @Override
    protected void clientException(ConnectionToClient client, Throwable exception) {
        super.clientException(client, exception);
        String host = (String) client.getInfo("host");
        String ip = (String) client.getInfo("ip");
        if (host == null) host = host(client);
        if (ip == null) ip = ip(client);
        log("Client exception: " + host + " (" + ip + "): " + exception.getMessage());
        if (controller != null) controller.onClientDisconnected(host, ip);
    }

    /**
     * Main message handler. Decodes request into {@link Envelope} and routes by {@link OpCode}.
     *
     * @param msg    incoming object from client (usually {@link KryoMessage} or {@link Envelope})
     * @param client client connection
     */
    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        try {
            Envelope req = unwrapToEnvelope(msg);
            if (req == null) {
                sendError(client, OpCode.ERROR, "Bad message format (expected Envelope via KryoMessage).");
                return;
            }

            switch (req.getOp()) {
                // --- RESERVATIONS ---
                case REQUEST_RESERVATIONS_LIST -> handleReservationsList(req, client);
                case REQUEST_REGISTER_CUSTOMER -> handleRegisterCustomer(req, client);
                case REQUEST_SUBSCRIBERS_LIST -> handleSubscribersList(req, client);
                case REQUEST_AGENT_RESERVATIONS_LIST -> handleAgentReservationsList(req, client);
                case REQUEST_MAKE_RESERVATION -> handleMakeReservation(req, client);
                case REQUEST_CHECK_AVAILABILITY -> handleCheckAvailability(req, client);
                case REQUEST_CANCEL_RESERVATION -> handleCancelReservation(req, client);

                // --- LOGIN ---
                case REQUEST_LOGIN_SUBSCRIBER -> handleLoginSubscriber(req, client);
                case REQUEST_LOGIN_STAFF      -> handleLoginStaff(req, client);
                
                // --- TERMINAL ---
                case REQUEST_TERMINAL_VALIDATE_CODE -> handleTerminalValidateCode(req, client);
                case REQUEST_TERMINAL_CHECK_IN -> handleTerminalCheckIn(req, client);
                case REQUEST_TERMINAL_CANCEL_RESERVATION -> handleTerminalCancelReservation(req, client);
                case REQUEST_TERMINAL_GET_SUBSCRIBER_ACTIVE_CODES ->handleTerminalGetSubscriberActiveCodes(req, client);
                
                // --- WAITING LIST (Fixed Logic) ---
                case REQUEST_WAITING_LIST -> handlgeteWaitingList(req, client); // Agent Viewing List
                case REQUEST_WAITING_ADD  -> handleWaitingList(req, client); // Customer Joining
                case REQUEST_WAITING_REMOVE -> handleRemoveWaitingCustomer(req, client); // <--- ADD THIS
                case REQUEST_LEAVE_WAITING_LIST -> handleLeaveWaitingList(req, client);
                
                // --- DASHBOARD: CURRENT DINERS ---
                case REQUEST_CURRENT_DINERS -> handleCurrentDiners(req, client);
                
                // --- DASHBOARD: TABLES ---
                case REQUEST_TABLES_GET -> handleGetTables(client);
                case REQUEST_TABLE_ADD -> handleAddTable(req, client);
                case REQUEST_TABLE_REMOVE -> handleRemoveTable(req, client);
                case REQUEST_TABLE_UPDATE -> handleUpdateTable(req, client);
                
                // --- DASHBOARD: OPENING HOURS ---
                case REQUEST_OPENING_HOURS_GET -> handleGetOpeningHours(client);
                case REQUEST_OPENING_HOURS_UPDATE -> handleUpdateOpeningHours(req, client);
                case REQUEST_OPENING_HOURS_ADD_SPECIAL -> handleAddSpecialHour(req, client);
                case REQUEST_OPENING_HOURS_REMOVE -> handleRemoveSpecialHour(req, client);
                case REQUEST_TODAY_HOURS -> handleGetTodayHours(client);
                
                // --- DASHBOARD: REPORTS ---
                case REQUEST_REPORT_PERFORMANCE -> handleReportPerformance(req,client);
                case REQUEST_REPORT_ACTIVITY -> handleReportActivity(req,client);

                // --- BILLING / HISTORY / PROFILE ---
                case REQUEST_HISTORY_GET -> sendOk(client, OpCode.RESPONSE_HISTORY_GET, new ArrayList<>());
                case REQUEST_BILL_GET_BY_CODE -> handleBillGetByCode(req, client);
                case REQUEST_PAY_BILL        -> handlePayBill(req, client);
                
                case REQUEST_GET_PROFILE -> handleGetProfile(req, client);
                case REQUEST_UPDATE_PROFILE -> handleUpdateProfile(req, client);
                case REQUEST_RECOVER_CONFIRMATION_CODE -> handleRecoverConfirmationCode(req, client);
                case REQUEST_GET_AVAILABLE_TIMES -> handleGetAvailableTimes(req, client);
                
                case REQUEST_SUBSCRIBER_HISTORY -> handleSubscriberHistory(req, client);
                
                case REQUEST_TERMINAL_RESOLVE_SUBSCRIBER_QR -> handleTerminalResolveSubscriberQR(req, client);


                default -> sendError(client, OpCode.ERROR, "Unknown op: " + req.getOp());
            }

        } catch (Exception e) {
            log("Error handling message: " + e.getMessage());
            try {
                sendError(client, OpCode.ERROR, "Server exception: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
    /**
     * Converts incoming object into {@link Envelope}.
     * Supports direct {@link Envelope} or {@link KryoMessage} that contains an Envelope payload.
     *
     * @param msg received message object
     * @return envelope or null if cannot decode
     */
    private Envelope unwrapToEnvelope(Object msg) {
        try {
            if (msg instanceof Envelope e) return e;

            if (msg instanceof KryoMessage km) {
                Object obj = KryoUtil.fromBytes(km.getPayload());
                if (obj instanceof Envelope e) return e;
            }
        } catch (Exception ex) {
            log("Decode error: " + ex.getMessage());
        }
        return null;
    }

    /* ==================== Envelope replies ==================== */
    /**
     * Sends a successful response.
     *
     * @param client  target client
     * @param op      response opcode
     * @param payload response payload object
     * @throws IOException if send fails
     */
    private void sendOk(ConnectionToClient client, OpCode op, Object payload) throws IOException {
        Envelope resp = Envelope.ok(op, payload);
        sendEnvelope(client, resp);
    }
    /**
     * Sends an error response.
     *
     * @param client  target client
     * @param op      response opcode (usually ERROR or specific response)
     * @param message human readable error message
     * @throws IOException if send fails
     */
    private void sendError(ConnectionToClient client, OpCode op, String message) throws IOException {
        Envelope resp = Envelope.error(message);
        resp.setOp(op);
        sendEnvelope(client, resp);
    }

    /**
     * Serializes envelope via Kryo and sends it.
     *
     * @param client target client
     * @param env    envelope to send
     * @throws IOException if sending fails
     */
    private void sendEnvelope(ConnectionToClient client, Envelope env) throws IOException {
        byte[] bytes = KryoUtil.toBytes(env);
        client.sendToClient(new KryoMessage("ENVELOPE", bytes));
    }

    /* ==================== Handlers ==================== */
    /**
     * Returns active reservation/waiting codes for a subscriber username (terminal screen use).
     */
    private void handleTerminalGetSubscriberActiveCodes(Envelope req, ConnectionToClient client) {
        try {
            String username = (String) req.getPayload();
            if (username == null || username.isBlank()) {
                sendOk(client,
                    OpCode.RESPONSE_TERMINAL_GET_SUBSCRIBER_ACTIVE_CODES,
                    new ArrayList<>());
                return;
            }

            var items = DataBase.dao.UserActivityDAO
                    .listActiveItemsBySubscriberUsername(username);

            sendOk(client,
                OpCode.RESPONSE_TERMINAL_GET_SUBSCRIBER_ACTIVE_CODES,
                items == null ? new ArrayList<>() : items);

        } catch (Exception e) {
            try {
                sendOk(client,
                    OpCode.RESPONSE_TERMINAL_GET_SUBSCRIBER_ACTIVE_CODES,
                    new ArrayList<>());
            } catch (Exception ignored) {}
        }
    }

    /**
     * Resolves subscriber QR (barcode) into username and list of active terminal items.
     * Response payload is Object[] { username, items } or an error string.
     */
    private void handleTerminalResolveSubscriberQR(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);
            String barcode = (payload instanceof String s) ? s.trim() : "";

            if (barcode.isBlank()) {
                sendOk(client,
                    OpCode.RESPONSE_TERMINAL_RESOLVE_SUBSCRIBER_QR,
                    "Invalid QR value.");
                return;
            }

            // 1️⃣ QR decides if this is a subscriber
            String username = SubscriberDAO.getUsernameByBarcodeData(barcode);

            if (username == null || username.isBlank()) {
                // ❌ Not a subscriber QR
                sendOk(client,
                    OpCode.RESPONSE_TERMINAL_RESOLVE_SUBSCRIBER_QR,
                    "This QR does not belong to a subscriber.");
                return;
            }

            // 2️⃣ Subscriber found → fetch ALL active items
            List<TerminalActiveItemDTO> items =
                UserActivityDAO.listActiveItemsBySubscriberUsername(username);

            if (items == null) items = new ArrayList<>();
            else items = new ArrayList<>(items);

            sendOk(client, OpCode.RESPONSE_TERMINAL_RESOLVE_SUBSCRIBER_QR, new Object[]{ username, items });

        } catch (Exception e) {
            try {
                sendOk(client,
                    OpCode.RESPONSE_TERMINAL_RESOLVE_SUBSCRIBER_QR,
                    "Server error: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
    /**
     * Returns available reservation time slots for a given date string (YYYY-MM-DD).
     */
    
    private void handleGetAvailableTimes(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);

            // payload is expected: String "YYYY-MM-DD"
            String dateStr = (payload instanceof String s) ? s.trim() : "";
            if (dateStr.isBlank()) {
                sendOk(client, OpCode.RESPONSE_GET_AVAILABLE_TIMES, new ArrayList<>());
                return;
            }

            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception ex) {
                sendOk(client, OpCode.RESPONSE_GET_AVAILABLE_TIMES, new ArrayList<>());
                return;
            }

            // Slot interval = 30 minutes, dining window = 120 minutes
            List<String> times = OpeningHoursDAO.getAvailableTimeSlots(date, 30, 120);

            sendOk(client, OpCode.RESPONSE_GET_AVAILABLE_TIMES, times);

        } catch (Exception e) {
            try {
                sendOk(client, OpCode.RESPONSE_GET_AVAILABLE_TIMES, new ArrayList<>());
            } catch (Exception ignored) {}
        }
    }
    /**
     * Returns subscriber reservation history + visits list.
     */
    private void handleSubscriberHistory(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);
            String username = (payload instanceof String s) ? s.trim() : null;

            if (username == null || username.isEmpty()) {
                // Send empty history if no username
                sendOk(client, OpCode.RESPONSE_SUBSCRIBER_HISTORY, 
                       new common.dto.HistoryDTO(new ArrayList<>(), new ArrayList<>()));
                return;
            }

            // 1. Fetch Reservations (Using the JOIN query)
            List<ReservationDTO> reservations = ReservationDAO.getReservationsBySubscriberForStaff(username);

            // 2. Fetch Visits (Using the JOIN query)
            List<String> visits = VisitDAO.getVisitsBySubscriber(username);

            // 3. Send Response
            common.dto.HistoryDTO history = new common.dto.HistoryDTO(reservations, visits);
            sendOk(client, OpCode.RESPONSE_SUBSCRIBER_HISTORY, history);

        } catch (Exception e) {
            log("Error fetching history: " + e.getMessage());
            try {
                sendError(client, OpCode.RESPONSE_SUBSCRIBER_HISTORY, "Server error: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
    /**
     * Cancels reservation by confirmation code (terminal cancel flow).
     */
    private void handleTerminalCancelReservation(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);
            String code = (payload instanceof String s) ? s.trim() : "";

            if (code.isBlank()) {
                sendOk(client, OpCode.RESPONSE_TERMINAL_CANCEL_RESERVATION, "Missing confirmation code.");
                return;
            }

            ReservationDAO.CancelByCodeResult r = ReservationDAO.cancelReservationByCode(code);

            sendOk(client, OpCode.RESPONSE_TERMINAL_CANCEL_RESERVATION, r.message);

        } catch (Exception e) {
            try {
                sendOk(client, OpCode.RESPONSE_TERMINAL_CANCEL_RESERVATION,
                        "Server error: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
    /**
     * Recovers an active confirmation code by email/phone (lost-code flow).
     * Sends the code by email (and SMS stub).
     */
    private void handleRecoverConfirmationCode(Envelope req, ConnectionToClient client) {
        try {
            Object payload = req.getPayload();
            String contact = (payload instanceof String s) ? s.trim() : "";

            if (contact.isBlank()) {
                sendOk(client, OpCode.RESPONSE_RECOVER_CONFIRMATION_CODE,
                        "Email or phone is required.");
                return;
            }

            UserActivityDAO.LostCodeResult r =
                    UserActivityDAO.findActiveCodeByContact(contact);

            if (r == null) {
                sendOk(client, OpCode.RESPONSE_RECOVER_CONFIRMATION_CODE,
                        "No active reservation or waiting list found.");
                return;
            }

            if (r.email != null && !r.email.isBlank()) {
                if ("RESERVATION".equals(r.type)) {
                    EmailService.sendReservationConfirmation(r.email, r.code);
                    EmailService.smsStub(r.phone, "Your reservation code :  " + r.code);
                } else {
                    EmailService.sendWaitingTableReady(r.email, r.code);
                    EmailService.smsStub(r.phone, "Your waiting list code : " + r.code);
                }
            }

            sendOk(client, OpCode.RESPONSE_RECOVER_CONFIRMATION_CODE,
                    "Confirmation code sent to your email.");

        } catch (Exception e) {
            try {
                sendOk(client, OpCode.RESPONSE_RECOVER_CONFIRMATION_CODE,
                        "Server error: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    /**
     * Retrieves bill by confirmation code.
     * Response payload is Object[] { ok, alreadyPaid, message, BillDTO }.
     */
    private void handleBillGetByCode(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);
            String code = (payload instanceof String s) ? s.trim() : "";

            BillDAO.BillLookupResult r = BillDAO.getBillByConfirmationCode(code);

            // Object[] { ok, alreadyPaid, message, BillDTO }
            Object[] resp = new Object[] { r.ok, r.alreadyPaid, r.message, r.bill };
            sendOk(client, OpCode.RESPONSE_BILL_GET_BY_CODE, resp);

        } catch (Exception e) {
            try {
                sendOk(client, OpCode.RESPONSE_BILL_GET_BY_CODE,
                        new Object[] { false, false, "Server error: " + e.getMessage(), null });
            } catch (Exception ignored) {}
        }
    }
    /**
     * Pays bill by confirmation code.
     * Accepts payload: String code OR Object[] { code, method }.
     * Response payload is Object[] { ok, message, tableId }.
     */
    private void handlePayBill(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);

            // Accept String code OR Object[] { code, method }
            String code;
            if (payload instanceof String s) code = s.trim();
            else if (payload instanceof Object[] arr && arr.length >= 1) code = String.valueOf(arr[0]).trim();
            else {
                sendOk(client, OpCode.RESPONSE_PAY_BILL, new Object[] { false, "Bad payload.", null });
                return;
            }

            BillDAO.PayBillResult r = BillDAO.payBillByConfirmationCode(code);

            // Object[] { ok, message, tableId }
            sendOk(client, OpCode.RESPONSE_PAY_BILL, new Object[] { r.ok, r.message, r.tableId });

        } catch (Exception e) {
            try {
                sendOk(client, OpCode.RESPONSE_PAY_BILL, new Object[] { false, "Server error: " + e.getMessage(), null });
            } catch (Exception ignored) {}
        }
    }
    /**
     * Loads subscriber profile by member code.
     */
    private void handleGetProfile(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);
            String memberCode = (payload instanceof String s) ? s.trim() : null;

            if (memberCode == null || memberCode.isBlank()) {
                sendOk(client, OpCode.RESPONSE_GET_PROFILE, null);
                return;
            }

            ProfileDTO dto = SubscriberDAO.getProfileByMemberCode(memberCode);
            sendOk(client, OpCode.RESPONSE_GET_PROFILE, dto); // dto may be null

        } catch (Exception e) {
            try {
                sendOk(client, OpCode.RESPONSE_GET_PROFILE, null);
            } catch (Exception ignored) {}
        }
    }


    /**
     * Updates subscriber profile (by member number/member_code).
     * Payload must be {@link ProfileDTO}.
     */
    private void handleUpdateProfile(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);

            if (!(payload instanceof ProfileDTO dto)) {
                sendOk(client, OpCode.RESPONSE_UPDATE_PROFILE, "Bad payload (expected ProfileDTO).");
                return;
            }

            // memberNumber is the primary key (unchangeable)
            String memberNumber = dto.getMemberNumber() == null ? "" : dto.getMemberNumber().trim();
            if (memberNumber.isBlank()) {
                sendOk(client, OpCode.RESPONSE_UPDATE_PROFILE, "Missing member number.");
                return;
            }

            // basic validation
            String fullName = dto.getFullName() == null ? "" : dto.getFullName().trim();
            String phone    = dto.getPhone() == null ? "" : dto.getPhone().trim();
            String email    = dto.getEmail() == null ? "" : dto.getEmail().trim();

            if (fullName.isBlank() || phone.isBlank() || email.isBlank()) {
                sendOk(client, OpCode.RESPONSE_UPDATE_PROFILE, "Full name / phone / email are required.");
                return;
            }

            boolean ok = SubscriberDAO.updateProfileByMemberCode(dto);

            sendOk(client, OpCode.RESPONSE_UPDATE_PROFILE,
                    ok ? "✅ Profile updated." : "❌ Update failed.");

        } catch (Exception e) {
            try {
                sendOk(client, OpCode.RESPONSE_UPDATE_PROFILE, "Server error: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    /**
     * Allows a user to leave the waiting list by confirmation code.
     * Blocks cancel if a visit already exists (checked-in).
     */
    private void handleLeaveWaitingList(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);

            // Expected payload: Object[] { role, username, confirmationCode }
            if (!(payload instanceof Object[] arr) || arr.length < 3) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Bad payload.");
                return;
            }

            String role = arr[0] == null ? "" : arr[0].toString();
            String username = arr[1] == null ? "" : arr[1].toString();
            String code = arr[2] == null ? "" : arr[2].toString().trim();

            if (code.isBlank()) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Missing waiting code.");
                return;
            }

            if ("SUBSCRIBER".equalsIgnoreCase(role) && username.isBlank()) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Missing subscriber username.");
                return;
            }

            // Load entry
            WaitingListDTO w = WaitingListDAO.getByCode(code);
            if (w == null) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Waiting code not found.");
                return;
            }

            String st = w.getStatus() == null ? "" : w.getStatus().trim();

            if ("CANCELED".equalsIgnoreCase(st)) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Already canceled.");
                return;
            }

            // ✅ Option A: allow cancel in WAITING or ASSIGNED
            if (!"WAITING".equalsIgnoreCase(st) && !"ASSIGNED".equalsIgnoreCase(st)) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Invalid status: " + st);
                return;
            }

            // ✅ Block cancel if already checked-in (Visit exists)
            boolean hasVisit = VisitDAO.existsVisitForWaitingId(w.getId());
            if (hasVisit) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Already checked-in. Can't cancel now.");
                return;
            }

            boolean ok = WaitingListDAO.cancelAndReleaseTablesByCode(code);
            if (!ok) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Cancel failed (status changed).");
                return;
            }

            w.setStatus("CANCELED");
            sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, w);

        } catch (Exception e) {
            try {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Server error: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
    /**
     * Adds a new waiting-list entry for subscriber or guest.
     * Also stores contact in user_activity for later notifications.
     */
    private void handleWaitingList(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);

            if (!(payload instanceof Object[] arr) || arr.length < 3 || !(arr[2] instanceof WaitingListDTO dto)) {
                sendOk(client, OpCode.RESPONSE_WAITING_LIST, "Bad payload.");
                return;
            }

            String role = arr[0] == null ? "" : arr[0].toString();
            String username = arr[1] == null ? "" : arr[1].toString();
            
         // Server/BistroServer.java

            Timestamp nowCheck = new Timestamp(System.currentTimeMillis());
            if (!OpeningHoursDAO.isOpenForReservation(nowCheck, 120)) {
                 sendOk(client, OpCode.RESPONSE_WAITING_LIST, "Restaurant is closed (or closing soon).");
                 return;
            }
            
            int people = dto.getPeopleCount();
            if (people <= 0) {
                sendOk(client, OpCode.RESPONSE_WAITING_LIST, "People count must be > 0.");		
                return;
            }

            
            int totalSeats = RestaurantTableDAO.getTotalSeats();
            if (totalSeats <= 0) {
                sendOk(client, OpCode.RESPONSE_WAITING_LIST, "Restaurant tables are not configured. Please contact staff.");
                return;
            }

            if (people > totalSeats) {
                String msg = "Group size (" + people + ") exceeds restaurant capacity (" + totalSeats + " seats). "
                        + "Please split into smaller groups or contact staff.";
                sendOk(client, OpCode.RESPONSE_WAITING_LIST, msg);
                return;
            }

            String email;
            String phone;

            if ("SUBSCRIBER".equalsIgnoreCase(role)) {
                if (username.isBlank()) {
                    sendOk(client, OpCode.RESPONSE_WAITING_LIST, "Missing subscriber username.");
                    return;
                }

                email = DataBase.dao.SubscriberDAO.getEmailByUsername(username);

                phone = SubscriberDAO.getPhoneByUsername(username);
                if (phone == null) phone = "";

                if (email == null || email.isBlank()) {
                    sendOk(client, OpCode.RESPONSE_WAITING_LIST, "Subscriber email not found.");
                    return;
                }
            } else {
             // CUSTOMER: must provide at least ONE contact method (email OR phone)
                email = dto.getEmail() == null ? "" : dto.getEmail().trim();
                phone = dto.getPhone() == null ? "" : dto.getPhone().trim();

                if (email.isBlank() || phone.isBlank()) {
                    sendOk(client, OpCode.RESPONSE_WAITING_LIST, "Email and phone is required for customers.");
                    return;
                }
            }

            Timestamp now = new Timestamp(System.currentTimeMillis());

            // ✅ Insert ONCE with unique code retry
            String code = null;
            int waitingId = -1;

            for (int i = 0; i < 5; i++) {
                code = "W" + java.util.UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 8)
                        .toUpperCase();

                try {
                    waitingId = WaitingListDAO.insertWaitingReturnId(
                            people,
                            now,
                            "WAITING",
                            code
                    );
                    if (waitingId > 0) break; // success
                } catch (Exception ex) {
                    // if duplicate code, retry
                    String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                    if (!msg.contains("duplicate")) {
                        throw ex;
                    }
                }
            }

            if (waitingId <= 0 || code == null) {
                sendOk(client, OpCode.RESPONSE_WAITING_LIST, "Failed to generate unique waiting code. Please try again.");
                return;
            }

            if ("SUBSCRIBER".equalsIgnoreCase(role)) {
                UserActivityDAO.insertSubscriberWaitingActivity(waitingId, username);
            } else {
                UserActivityDAO.insertWaitingActivity(waitingId, email, phone);
            }

            // ✅ build response DTO
            WaitingListDTO resp = new WaitingListDTO();
            resp.setId(waitingId);
            resp.setPeopleCount(people);
            resp.setStatus("WAITING");
            resp.setConfirmationCode(code);
            resp.setEmail(email);
            resp.setPhone(phone);

            sendOk(client, OpCode.RESPONSE_WAITING_LIST, resp);

        } catch (Exception e) {
            try {
                sendOk(client, OpCode.RESPONSE_WAITING_LIST, "Server error: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
    /**
     * Cancels reservation by reservationId with identity check.
     * Payload expected: Object[] { role, username, guestEmail, guestPhone, reservationId }.
     */
    @SuppressWarnings("unchecked")
    private void handleCancelReservation(Envelope req, ConnectionToClient client) {
        try {
            Object payload = req.getPayload();

            // Expected payload: Object[] { role, username, guestEmail, guestPhone, reservationId }
            if (!(payload instanceof Object[] arr) || arr.length < 5) {
                sendOk(client, OpCode.RESPONSE_CANCEL_RESERVATION,
                        "Bad payload. Expected {role, username, email, phone, reservationId}.");
                return;
            }

            String role = arr[0] == null ? null : String.valueOf(arr[0]);
            String username = arr[1] == null ? null : String.valueOf(arr[1]);
            String guestEmail = arr[2] == null ? null : String.valueOf(arr[2]);
            String guestPhone = arr[3] == null ? null : String.valueOf(arr[3]);

            int reservationId;
            try {
                reservationId = (arr[4] instanceof Number n) ? n.intValue() : Integer.parseInt(String.valueOf(arr[4]));
            } catch (Exception ex) {
                sendOk(client, OpCode.RESPONSE_CANCEL_RESERVATION, "Bad reservation id.");
                return;
            }

            boolean isSubscriber = role != null && role.equalsIgnoreCase("SUBSCRIBER");
            boolean isCustomer = role != null && role.equalsIgnoreCase("CUSTOMER");

            if (!isSubscriber && !isCustomer) {
                sendOk(client, OpCode.RESPONSE_CANCEL_RESERVATION, "Unknown role.");
                return;
            }

            if (isSubscriber) {
                if (username == null || username.isBlank()) {
                    sendOk(client, OpCode.RESPONSE_CANCEL_RESERVATION, "Missing subscriber username.");
                    return;
                }
            } else {
                // Customer must prove identity (email + phone)
                if (guestEmail == null || guestEmail.isBlank() || guestPhone == null || guestPhone.isBlank()) {
                    sendOk(client, OpCode.RESPONSE_CANCEL_RESERVATION, "Email + phone are required for customers.");
                    return;
                }
            }

            // Call DAO
            // This method should:
            // 1) Verify reservation belongs to the requester
            // 2) Verify status is cancel-able (e.g., CONFIRMED)
            // 3) Update status to CANCELLED
            boolean ok = ReservationDAO.cancelReservation(
                    reservationId,
                    role,
                    username,
                    guestEmail,
                    guestPhone
            );

            if (ok) {
                sendOk(client, OpCode.RESPONSE_CANCEL_RESERVATION, "Reservation cancelled.");
            } else {
                sendOk(client, OpCode.RESPONSE_CANCEL_RESERVATION, "Cancel failed (not found / not yours / not active).");
            }


        } catch (Exception e) {
            log("Cancel reservation failed: " + e.getMessage());
            try {
                sendOk(client, OpCode.RESPONSE_CANCEL_RESERVATION, "Server error: " + e.getMessage());
            } catch (Exception ignored) {
                log("Also failed to send response to client: " + ignored.getMessage());
            }
        }
    }
    /**
     * Returns current diners (active visits) for dashboard.
     */
    private void handleCurrentDiners(Envelope req, ConnectionToClient client) {
        try {
            // Log that we started the request (This helps debug!)
            log("Received request for Current Diners from " + host(client));

            List<common.dto.CurrentDinersDTO> list = DataBase.dao.VisitDAO.getActiveDiners();
            sendOk(client, OpCode.RESPONSE_CURRENT_DINERS, list);
            
            log("Sent " + list.size() + " current diners.");
        } catch (Exception e) {
            log("Error fetching current diners: " + e.getMessage());
            e.printStackTrace(); // Print error to console
        }
    }
    /**
     * Sends list of restaurant tables.
     */
    private void handleGetTables(ConnectionToClient client) {
        try {
            List<common.dto.RestaurantTableDTO> list = DataBase.dao.RestaurantTableDAO.getAllTables();
            sendOk(client, OpCode.RESPONSE_TABLES_GET, list);
        } catch (Exception e) {
            try { sendError(client, OpCode.ERROR, "Fetch tables failed: " + e.getMessage()); } catch (Exception ignored) {}
        }
    }
    /**
     * Adds a table (or updates it depending on DAO behavior).
     */
    private void handleAddTable(Envelope req, ConnectionToClient client) {
        try {
            if (req.getPayload() instanceof common.dto.RestaurantTableDTO dto) {
                // Default status FREE if not set
                String status = (dto.getStatus() == null || dto.getStatus().isEmpty()) ? "FREE" : dto.getStatus();
                DataBase.dao.RestaurantTableDAO.insertTable(dto.getTableId(), dto.getSeats(), status);
                sendOk(client, OpCode.RESPONSE_TABLE_ADD, "Table added/updated successfully.");
            }
        } catch (Exception e) {
            try { sendError(client, OpCode.ERROR, "Add table failed: " + e.getMessage()); } catch (Exception ignored) {}
        }
    }
    /**
     * Removes a table by tableId.
     */
    private void handleRemoveTable(Envelope req, ConnectionToClient client) {
        try {
            String tableId = (String) req.getPayload();
            boolean ok = DataBase.dao.RestaurantTableDAO.deleteTable(tableId);
            if (ok) sendOk(client, OpCode.RESPONSE_TABLE_REMOVE, "Table removed.");
            else sendError(client, OpCode.ERROR, "Table not found or could not be removed.");
        } catch (Exception e) {
            try { sendError(client, OpCode.ERROR, "Remove table failed: " + e.getMessage()); } catch (Exception ignored) {}
        }
    }
    /**
     * Updates seats of an existing table.
     */
    private void handleUpdateTable(Envelope req, ConnectionToClient client) {
        try {
            common.dto.RestaurantTableDTO dto = (common.dto.RestaurantTableDTO) req.getPayload();
            boolean ok = DataBase.dao.RestaurantTableDAO.updateTableSeats(dto.getTableId(), dto.getSeats());
            if (ok) sendOk(client, OpCode.RESPONSE_TABLE_UPDATE, "Table updated.");
            else sendError(client, OpCode.ERROR, "Update failed.");
        } catch (Exception e) {
            try { sendError(client, OpCode.ERROR, "Update table failed: " + e.getMessage()); } catch (Exception ignored) {}
        }
    }
    /**
     * Returns all opening hours rows.
     */
    private void handleGetOpeningHours(ConnectionToClient client) {
        try {
            List<common.dto.OpeningHoursDTO> list = DataBase.dao.OpeningHoursDAO.getAllOpeningHours();
            sendOk(client, OpCode.RESPONSE_OPENING_HOURS_GET, list);
        } catch (Exception e) {
            try { sendError(client, OpCode.ERROR, "Fetch hours failed: " + e.getMessage()); } catch (Exception ignored) {}
        }
    }
    /**
     * Updates opening hour row by ID.
     */
    private void handleUpdateOpeningHours(Envelope req, ConnectionToClient client) {
        try {
            common.dto.OpeningHoursDTO dto = (common.dto.OpeningHoursDTO) req.getPayload();
            boolean ok = DataBase.dao.OpeningHoursDAO.updateOpeningHour(dto.getHoursId(), dto.getOpenTime(), dto.getCloseTime());
            if (ok) sendOk(client, OpCode.RESPONSE_OPENING_HOURS_UPDATE, "Hours updated successfully.");
            else sendError(client, OpCode.ERROR, "Update failed.");
        } catch (Exception e) {
            try { sendError(client, OpCode.ERROR, "Update error: " + e.getMessage()); } catch (Exception ignored) {}
        }
    }
    /**
     * Inserts a special opening-hours row for a specific date.
     */
    private void handleAddSpecialHour(Envelope req, ConnectionToClient client) {
        try {
            common.dto.OpeningHoursDTO dto = (common.dto.OpeningHoursDTO) req.getPayload();
            // Insert into DB
            DataBase.dao.OpeningHoursDAO.insertSpecialHour(dto.getSpecialDate(), dto.getDayOfWeek(), dto.getOpenTime(), dto.getCloseTime());
            sendOk(client, OpCode.RESPONSE_OPENING_HOURS_ADD_SPECIAL, "Special date added.");
        } catch (Exception e) {
            try { sendError(client, OpCode.ERROR, "Add special date failed: " + e.getMessage()); } catch (Exception ignored) {}
        }
    }
    /**
     * Removes opening-hours row (special or regular) by ID.
     */
    private void handleRemoveSpecialHour(Envelope req, ConnectionToClient client) {
        try {
            int id = (int) req.getPayload();
            boolean ok = DataBase.dao.OpeningHoursDAO.deleteOpeningHour(id);
            if (ok) sendOk(client, OpCode.RESPONSE_OPENING_HOURS_REMOVE, "Special date removed.");
            else sendError(client, OpCode.ERROR, "Could not remove (might differ from ID).");
        } catch (Exception e) {
            try { sendError(client, OpCode.ERROR, "Remove failed: " + e.getMessage()); } catch (Exception ignored) {}
        }
    }
    /**
     * Returns opening-hours string for today (used in UI for quick display).
     */
    private void handleGetTodayHours(ConnectionToClient client) {
        try {
            String hours = DataBase.dao.OpeningHoursDAO.getHoursForDate(java.time.LocalDate.now());
            sendOk(client, OpCode.RESPONSE_TODAY_HOURS, hours);
        } catch (Exception e) {
            try { sendError(client, OpCode.ERROR, "Failed to get today's hours"); } catch (Exception ignored) {}
        }
    }
    
    
    /**
     * Creates performance report data for given month/year.
     */
    private void handleReportPerformance(Envelope req, ConnectionToClient client) {
        try {
            common.dto.ReportRequestDTO dto = (common.dto.ReportRequestDTO) req.getPayload();
            List<common.dto.ReportDTO> data = DataBase.dao.ReportDAO.getPerformanceReport(dto.getMonth(), dto.getYear());
            sendOk(client, OpCode.RESPONSE_REPORT_PERFORMANCE, data);
        } catch (Exception e) {
            e.printStackTrace();
            try { sendError(client, OpCode.ERROR, "Perf Report Failed"); } catch (Exception ignored) {}
        }
    }
    /**
     * Creates subscriber activity report data for given month/year.
     */
    private void handleReportActivity(Envelope req, ConnectionToClient client) {
        try {
            common.dto.ReportRequestDTO dto = (common.dto.ReportRequestDTO) req.getPayload();
            List<common.dto.ReportDTO> data = DataBase.dao.ReportDAO.getSubscriberActivityReport(dto.getMonth(), dto.getYear());
            sendOk(client, OpCode.RESPONSE_REPORT_ACTIVITY, data);
        } catch (Exception e) {
            e.printStackTrace();
            try { sendError(client, OpCode.ERROR, "Activity Report Failed"); } catch (Exception ignored) {}
        }
    }


    // -----------------------------------------------------------
    // 1. REGISTRATION (From your HEAD branch)
    // -----------------------------------------------------------
    /**
     * Registers a new subscriber (customer registration screen).
     * Payload must be {@link RegistrationDTO}.
     */
    private void handleRegisterCustomer(Envelope req, ConnectionToClient client) throws IOException {
        try {
            RegistrationDTO dto = (RegistrationDTO) req.getPayload();
            
            // Convert String date to java.sql.Date
            Date sqlBirthDate = null;
            try {
                sqlBirthDate = Date.valueOf(dto.getBirthDate());
            } catch (Exception e) {
                // Handle invalid date or set default
                sqlBirthDate = new Date(System.currentTimeMillis());
            }

            // Call the static DAO method
            SubscriberDAO.insertSubscriber(
                dto.getUsername(),
                dto.getPassword(),
                dto.getFullName(),
                dto.getPhone(),
                dto.getEmail(),
                sqlBirthDate
            );

            log("Registered new subscriber: " + dto.getUsername());
            sendOk(client, OpCode.RESPONSE_REGISTER_CUSTOMER, "Success");

        } catch (Exception e) {
            log("Error registering customer: " + e.getMessage());
            sendError(client, OpCode.ERROR, "Registration failed: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------
    // 2. MAKE RESERVATION (From the Main branch)
    // -----------------------------------------------------------
    /**
     * Validates email format with a simple regex.
     *
     * @param email raw email
     * @return true if matches pattern
     */
    private static boolean isValidEmailFormat(String email) {
        if (email == null) return false;
        String e = email.trim();
        return e.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    /**
     * Validates Israeli phone format: must start with 05 and be exactly 10 digits.
     *
     * @param phone raw phone string
     * @return true if matches pattern
     */
    private static boolean isValidPhone10Digits(String phone) {
        if (phone == null) return false;
        String p = phone.trim();
        return p.matches("^05\\d{8}$");
    }
    /**
     * Creates a reservation after validating constraints:
     * <ul>
     *   <li>guest must provide valid email + phone</li>
     *   <li>reservation up to 1 month ahead</li>
     *   <li>reservation at least 1 hour from now</li>
     *   <li>capacity check + alternative suggestions</li>
     * </ul>
     * Sends confirmation email/SMS stub on success.
     */
    private void handleMakeReservation(Envelope req, ConnectionToClient client) throws Exception {

        Object payloadObj = readEnvelopePayload(req);
        if (!(payloadObj instanceof MakeReservationRequestDTO dto)) {
            sendError(client, OpCode.RESPONSE_MAKE_RESERVATION, "Bad payload: expected MakeReservationRequestDTO");
            return;
        }

        if (dto.getNumOfCustomers() <= 0 || dto.getReservationTime() == null) {
            sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION, new MakeReservationResponseDTO(false, -1, null, "Invalid input."));
            return;
        }

        if (!dto.isSubscriber()) {
            String ge = dto.getGuestEmail() == null ? "" : dto.getGuestEmail().trim();
            String gp = dto.getGuestPhone() == null ? "" : dto.getGuestPhone().trim();

            if (!isValidEmailFormat(ge)) {
                sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION,
                        new MakeReservationResponseDTO(false, -1, null, "Invalid guest email."));
                return;
            }

            if (!isValidPhone10Digits(gp)) {
                sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION,
                        new MakeReservationResponseDTO(false, -1, null, "Invalid guest phone (must be 10 digits)."));
                return;
            }
        }

        // Logic check: Max 1 month ahead
        java.time.LocalDateTime maxLdt = java.time.LocalDateTime.now().plusMonths(1);
        Timestamp maxTime = Timestamp.valueOf(maxLdt);
        if (dto.getReservationTime().after(maxTime)) {
            sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION, new MakeReservationResponseDTO(false, -1, null, "Reservation can be made up to 1 month ahead."));
            return;
        }
        
     // Rule: reservation must be at least 1 hour from now
        java.time.LocalDateTime nowPlus1Hour = java.time.LocalDateTime.now().plusHours(1);
        Timestamp minAllowed = Timestamp.valueOf(nowPlus1Hour);

        if (dto.getReservationTime().before(minAllowed)) {
            sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION,
                   new MakeReservationResponseDTO(false, -1, null,
                           "Reservation must be at least 1 hour from now."));
            return;
        }
        
        if (!OpeningHoursDAO.isOpenForReservation(dto.getReservationTime(), 120)) {
            sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION,
                   new MakeReservationResponseDTO(false, -1, null,
                           "The restaurant is closed at the selected time."));
            return;
        }


        boolean canFit = ReservationDAO.canFitAtTime(dto.getReservationTime(), dto.getNumOfCustomers());

        if (!canFit) {
            List<Timestamp> suggestions =
                    findAlternativeTimes(dto.getReservationTime(), dto.getNumOfCustomers());
            sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION,
                    new MakeReservationResponseDTO(false, "No available seats at requested time.", suggestions));
            return;
        }

        // Create reservation
        ReservationDAO.CreateReservationResult r = reservationDAO.createReservationWithActivity(dto);
        
        // Send Email (Optional, wrapped in try/catch so it doesn't crash server)
        try {
            String toEmail;
            String phone;
            if (dto.isSubscriber()) {
                toEmail = DataBase.dao.SubscriberDAO.getEmailByUsername(dto.getSubscriberUsername());
                phone = SubscriberDAO.getPhoneByUsername(dto.getSubscriberUsername());
                } else {
                toEmail = dto.getGuestEmail();
                phone = dto.getGuestPhone();
            }
            if (toEmail != null && !toEmail.isBlank()) {
                EmailService.sendReservationConfirmation(toEmail, r.confirmationCode);
                EmailService.smsStub(phone,"Reservation has been made | code :" + r.confirmationCode);
            }
        } catch (Exception mailEx) {
            System.out.println("[EMAIL] Failed to send email: " + mailEx.getMessage());
        }

        sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION, new MakeReservationResponseDTO(true, r.reservationId, r.confirmationCode, "Reservation created successfully!"));
    }

    // -----------------------------------------------------------
    // 3. OTHER HANDLERS
    // -----------------------------------------------------------
    /**
     * Checks capacity availability without creating a reservation.
     * Returns alternative suggestions if cannot fit.
     */
    private void handleCheckAvailability(Envelope req, ConnectionToClient client) throws Exception {
        Object payloadObj = readEnvelopePayload(req);
        if (!(payloadObj instanceof MakeReservationRequestDTO dto)) {
            sendError(client, OpCode.RESPONSE_CHECK_AVAILABILITY, "Bad payload.");
            return;
        }

        if (dto.getNumOfCustomers() <= 0 || dto.getReservationTime() == null) {
            sendOk(client, OpCode.RESPONSE_CHECK_AVAILABILITY, new MakeReservationResponseDTO(false, -1, null, "Invalid input."));
            return;
        }
        

        boolean canFit = ReservationDAO.canFitAtTime(dto.getReservationTime(), dto.getNumOfCustomers());

        if (!canFit) {
            List<Timestamp> suggestions =
                    findAlternativeTimes(dto.getReservationTime(), dto.getNumOfCustomers());
            sendOk(client, OpCode.RESPONSE_CHECK_AVAILABILITY,
                    new MakeReservationResponseDTO(false, "No available seats at requested time.", suggestions));
            return;
        }

        sendOk(client, OpCode.RESPONSE_CHECK_AVAILABILITY,
                new MakeReservationResponseDTO(true, -1, null, "Available"));

    }
    /**
     * Validates a confirmation code at the terminal:
     * <ul>
     *   <li>first checks reservation code</li>
     *   <li>if not reservation → checks waiting-list code</li>
     *   <li>maps waiting-list status into valid/check-in allowed flags</li>
     * </ul>
     */
    private void handleTerminalValidateCode(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);
            String code = (payload instanceof String s) ? s : null;

            if (code == null || code.isBlank()) {
                sendOk(client, OpCode.RESPONSE_TERMINAL_VALIDATE_CODE,
                        new TerminalValidateResponseDTO(false, "Invalid confirmation code."));
                return;
            }

            code = code.trim();

            // 1) Try RESERVATION code (existing)
            TerminalValidateResponseDTO info = DataBase.dao.ReservationDAO.getTerminalInfoByCode(code);
            if (info != null && info.isValid()) {
                sendOk(client, OpCode.RESPONSE_TERMINAL_VALIDATE_CODE, info);
                return;
            }

         // 2) Try WAITING LIST code (NEW)
            WaitingListDTO w = WaitingListDAO.getByCode(code);
            if (w == null) {
                sendOk(client, OpCode.RESPONSE_TERMINAL_VALIDATE_CODE,
                        new TerminalValidateResponseDTO(false, "Code not found."));
                return;
            }

            TerminalValidateResponseDTO dto = new TerminalValidateResponseDTO();

            // Not a reservation
            dto.setReservationId(0);
            dto.setNumOfCustomers(w.getPeopleCount());
            dto.setStatus(w.getStatus());

            String st = (w.getStatus() == null) ? "" : w.getStatus().trim();

            if ("ASSIGNED".equalsIgnoreCase(st)) {
                dto.setValid(true);
                dto.setCheckInAllowed(true);
                dto.setMessage("Table is assigned. You may check-in now.");
            } else if ("WAITING".equalsIgnoreCase(st)) {
                dto.setValid(true);
                dto.setCheckInAllowed(false);
                dto.setMessage("You are in the waiting list. Please wait…");
            } else if ("CANCELED".equalsIgnoreCase(st)) {
                // ✅ IMPORTANT: expired/canceled should NOT be valid
                dto.setValid(false);
                dto.setCheckInAllowed(false);
                dto.setMessage("This waiting code has expired or was canceled.");
            } else {
                // Unknown status -> treat as invalid
                dto.setValid(false);
                dto.setCheckInAllowed(false);
                dto.setMessage("Invalid waiting list status.");
            }

            sendOk(client, OpCode.RESPONSE_TERMINAL_VALIDATE_CODE, dto);
            return;

        } catch (Exception e) {
            try {
                sendError(client, OpCode.RESPONSE_TERMINAL_VALIDATE_CODE, "Server error: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
    /**
     * Performs terminal check-in:
     * <ul>
     *   <li>reservation check-in: marks ARRIVED or PENDING (if no table yet)</li>
     *   <li>waiting-list check-in: delegates to {@link WaitingListDAO#checkInWaitingListByCode(String)}</li>
     * </ul>
     */
    private void handleTerminalCheckIn(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);
            String code = (payload instanceof String s) ? s : null;

            if (code == null || code.isBlank()) {
                sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_IN,
                        new TerminalValidateResponseDTO(false, "Invalid confirmation code."));
                return;
            }

            code = code.trim();
            
            // 1) RESERVATION check-in (keep your existing behavior)
            TerminalValidateResponseDTO info = DataBase.dao.ReservationDAO.getTerminalInfoByCode(code);
            if (info != null && info.isValid()) {
            	String tableId = ReservationDAO.markArrivedByCodeReturnTableId(code);
            	TerminalValidateResponseDTO dto = ReservationDAO.getTerminalInfoByCode(code);
            	dto.setValid(true);

            	if (tableId == null || tableId.isBlank()) {
            	    // no table -> the DAO should have switched it to PENDING
            	    dto.setStatus("PENDING");
            	    dto.setCheckInAllowed(false);
            	    dto.setTableId("-");
            	    dto.setMessage("No suitable table now. Please wait — you will be notified when a table is ready.");
            	} else {
            	    dto.setStatus("ARRIVED");
            	    dto.setCheckInAllowed(false);
            	    dto.setTableId(tableId);
            	    dto.setMessage("Checked-in successfully. Table: " + tableId);
            	}

            	sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_IN, dto);
            	return;

            }

            // 2) WAITING LIST check-in (ALL DB WORK INSIDE DAO)
            TerminalValidateResponseDTO dto = WaitingListDAO.checkInWaitingListByCode(code);

            sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_IN, dto);

        } catch (Exception e) {
            TerminalValidateResponseDTO err = new TerminalValidateResponseDTO();
            err.setValid(false);
            err.setMessage("Server error: " + e.getMessage());
            try {
                sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_IN, err);
            } catch (Exception ignored) {}
        }
    }


    /**
     * Handles subscriber login (returns member_code for profile flow).
     */
    private void handleLoginSubscriber(Envelope env, ConnectionToClient client) {
        try {
            var req = (common.dto.LoginRequestDTO) env.getPayload();

            boolean ok = DataBase.dao.SubscriberDAO.checkLogin(req.getUsername(), req.getPassword());

            LoginResponseDTO res;

            if (ok) {
                // member_code ONLY flow -> must fetch it and send it back
                String memberCode = DataBase.dao.SubscriberDAO.getMemberCodeByUsername(req.getUsername());

                if (memberCode == null || memberCode.isBlank()) {
                    // If member_code missing -> fail (because profile uses member_code)
                    res = new common.dto.LoginResponseDTO(
                            false,
                            "Login failed: member code not found for this user.",
                            null,
                            null,
                            null
                    );
                } else {
                    res = new common.dto.LoginResponseDTO(
                            true,
                            "Login success",
                            req.getUsername(),
                            "SUBSCRIBER",
                            memberCode.trim()
                    );
                }
            } else {
                res = new common.dto.LoginResponseDTO(
                        false,
                        "Invalid username or password",
                        null,
                        null,
                        null
                );
            }

            Envelope reply = Envelope.ok(OpCode.RESPONSE_LOGIN_SUBSCRIBER, res);
            client.sendToClient(new KryoMessage("ENVELOPE", KryoUtil.toBytes(reply)));

        } catch (Exception e) {
            try {
                LoginResponseDTO res = new common.dto.LoginResponseDTO(
                        false,
                        "Server error: " + e.getMessage(),
                        null,
                        null,
                        null
                );
                Envelope reply = Envelope.ok(OpCode.RESPONSE_LOGIN_SUBSCRIBER, res);
                client.sendToClient(new KryoMessage("ENVELOPE", KryoUtil.toBytes(reply)));
            } catch (Exception ignored) {}
        }
    }

    /**
     * Handles staff login and returns staff role (AGENT/MANAGER).
     */
    private void handleLoginStaff(Envelope env, ConnectionToClient client) {
        try {
            var req = (common.dto.LoginRequestDTO) env.getPayload();
            String role = DataBase.dao.StaffDAO.checkLoginAndGetRole(req.getUsername(), req.getPassword());

            LoginResponseDTO res = (role != null)
                    ? new common.dto.LoginResponseDTO(true, "Login success", req.getUsername(), role.trim(), null)
                    : new common.dto.LoginResponseDTO(false, "Invalid username or password", null, null, null);

            Envelope reply = Envelope.ok(OpCode.RESPONSE_LOGIN_STAFF, res);
            client.sendToClient(new KryoMessage("ENVELOPE", KryoUtil.toBytes(reply)));
        } catch (Exception e) {
            try {
                LoginResponseDTO res = new common.dto.LoginResponseDTO(false, "Server error: " + e.getMessage(), null, null, null);
                Envelope reply = Envelope.ok(OpCode.RESPONSE_LOGIN_STAFF, res);
                client.sendToClient(new KryoMessage("ENVELOPE", KryoUtil.toBytes(reply)));
            } catch (Exception ignored) {}
        }
    }

    /**
     * Returns reservation list for subscriber or guest based on role.
     * Payload expected: Object[] { role, username, email, phone }.
     */
    private void handleReservationsList(Envelope req, ConnectionToClient client) throws Exception {
        Object payload = readEnvelopePayload(req);
        Object[] arr = (Object[]) payload;
        String role = (String) arr[0];
        String username = (String) arr[1];
        String email = (String) arr[2];
        String phone = (String) arr[3];

        List<Reservation> rows;
        if ("SUBSCRIBER".equals(role)) {
            rows = reservationDAO.getReservationsBySubscriber(username);
        } else {
            rows = reservationDAO.getReservationsByGuest(email, phone);
        }

        List<Object> dtoList = new ArrayList<>();
        for (Reservation r : rows) {
            Object dto = toReservationDTO(r);
            if (dto != null) dtoList.add(dto);
        }
        sendOk(client, OpCode.RESPONSE_RESERVATIONS_LIST, dtoList);
    }

    /**
     * Sends list of subscribers for staff view.
     */
    private void handleSubscribersList(Envelope req,ConnectionToClient client) {
        try {
            List<SubscriberDTO> list = DataBase.dao.SubscriberDAO.getAllSubscribers();
            sendOk(client, OpCode.RESPONSE_SUBSCRIBERS_LIST, list);
            log("Sent " + list.size() + " subscribers to client.");
        } catch (Exception e) {
            log("Error fetching subscribers: " + e.getMessage());
            try { sendError(client, OpCode.ERROR, "Fetch failed"); } catch (Exception ignored) {}
        }
    }
    /**
     * Sends list of all reservations for agent dashboard.
     */
    private void handleAgentReservationsList(Envelope req,ConnectionToClient client) {
        try {
            List<Reservation> rows = reservationDAO.getAllReservations();
            List<ReservationDTO> dtoList = new ArrayList<>();
            for (Reservation r : rows) {
                String resTimeStr = (r.getReservationTime() != null) ? r.getReservationTime().toString() : "";
                String expTimeStr = (r.getExpiryTime() != null) ? r.getExpiryTime().toString() : "";

                dtoList.add(new ReservationDTO(
                    r.getReservationId(),      
                    r.getConfirmationCode(),   
                    resTimeStr,                
                    expTimeStr,                
                    r.getNumOfCustomers(),     
                    r.getStatus()              
                ));
            }
            sendOk(client, OpCode.RESPONSE_AGENT_RESERVATIONS_LIST, dtoList); 
            log("Sent all reservations to Agent.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Staff removes a waiting-list entry by ID (sets to CANCELED and releases reserved tables).
     */
    private void handleRemoveWaitingCustomer(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);
            
            // Staff sends just the ID (Integer)
            int waitingId = -1;
            if (payload instanceof Integer i) waitingId = i;
            else if (payload instanceof String s) waitingId = Integer.parseInt(s);
            
            if (waitingId <= 0) {
                sendOk(client, OpCode.RESPONSE_WAITING_REMOVE, "Invalid waiting ID.");
                return;
            }

            // Call the new DAO method
            boolean success = DataBase.dao.WaitingListDAO.cancelWaitingById(waitingId);

            if (success) {
                sendOk(client, OpCode.RESPONSE_WAITING_REMOVE, "Customer removed from waiting list.");
            } else {
                sendOk(client, OpCode.RESPONSE_WAITING_REMOVE, "Could not remove (already arrived or not found).");
            }

        } catch (Exception e) {
            try {
                sendOk(client, OpCode.RESPONSE_WAITING_REMOVE, "Server error: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
    /**
     * Returns waiting list items for agent view (WAITING/ASSIGNED).
     */
    private void handlgeteWaitingList(Envelope req,ConnectionToClient client) {
        try {
        	List<common.dto.WaitingListDTO> list = DataBase.dao.WaitingListDAO.getAllWaitingList();
            sendOk(client, OpCode.RESPONSE_WAITING_LIST, list); 
            log("Sent "+list.size()+" Waiting List items to Agent.");
        } catch (Exception e) {
            log("Error fetching waiting list: " + e.getMessage());
            e.printStackTrace(); // Good for debugging
            try { 
                sendError(client, OpCode.ERROR, "Fetch waiting list failed: " + e.getMessage()); 
            } catch (Exception ignored) {}
        }
    }

    /* ==================== Helpers ==================== */
    /**
     * Converts {@link Reservation} entity to ReservationDTO using reflection,
     * to remain compatible with different DTO versions.
     *
     * @param r reservation entity
     * @return DTO object or null on failure
     */
    private Object toReservationDTO(Reservation r) {
        try {
            Class<?> dtoCls = Class.forName("common.dto.ReservationDTO");
            Object dto = dtoCls.getDeclaredConstructor().newInstance();

            int reservationId = r.getReservationId();
            String confirmationCode = r.getConfirmationCode();
            Timestamp resTs = r.getReservationTime();
            Timestamp expTs = r.getExpiryTime();
            int customers = r.getNumOfCustomers();
            String status = r.getStatus();

            String resTimeStr = (resTs == null) ? "-" : resTs.toLocalDateTime().toString().replace('T', ' ');
            String expTimeStr = (expTs == null) ? "-" : expTs.toLocalDateTime().toString().replace('T', ' ');

            invoke(dto, "setReservationId", int.class, reservationId);
            invoke(dto, "setConfirmationCode", String.class, confirmationCode);
            invoke(dto, "setReservationTime", String.class, resTimeStr);
            invoke(dto, "setExpiryTime", String.class, expTimeStr);
            invoke(dto, "setNumOfCustomers", int.class, customers);
            invoke(dto, "setStatus", String.class, status);

            return dto;
        } catch (Exception ex) {
            log("toReservationDTO error: " + ex.getMessage());
            return null;
        }
    }
    /**
     * Reflection helper: invoke setter method if exists.
     */
    private void invoke(Object obj, String method, Class<?> param, Object value) {
        try {
            obj.getClass().getMethod(method, param).invoke(obj, value);
        } catch (Exception ignored) {}
    }
    /**
     * Reads payload value from envelope using common getter variations.
     *
     * @param env envelope object
     * @return payload object or null
     */
    private Object readEnvelopePayload(Envelope env) {
        Object val = tryInvoke(env, "getPayload");
        if (val != null) return val;
        val = tryInvoke(env, "getData");
        if (val != null) return val;
        val = tryInvoke(env, "getBody");
        return val;
    }

    /**
     * Tries to invoke a no-arg method by name.
     *
     * @param target object
     * @param methodName method to call
     * @return result or null if missing/fails
     */
    private Object tryInvoke(Object target, String methodName) {
        try {
            var m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }
    /**
     * Finds up to 5 alternative reservation timestamps around a requested time.
     * Filters by:
     * <ul>
     *   <li>minute alignment (00/30)</li>
     *   <li>not in the past</li>
     *   <li>not beyond 1 month</li>
     *   <li>opening hours window</li>
     *   <li>capacity availability</li>
     * </ul>
     *
     * @param requested requested timestamp
     * @param numCustomers group size
     * @return list of candidate timestamps
     * @throws Exception DAO checks may throw
     */
    private List<Timestamp> findAlternativeTimes(Timestamp requested, int numCustomers) throws Exception {
        List<Timestamp> alternatives = new ArrayList<>();
        int[] offsets = { -120 ,-90, -60, -30, 30, 60, 90 ,120};

        for (int minutes : offsets) {
            Timestamp candidate = Timestamp.valueOf(requested.toLocalDateTime().plusMinutes(minutes));
            
            int m = candidate.toLocalDateTime().getMinute();
            if (m != 0 && m != 30) continue;
            
            if (candidate.before(new Timestamp(System.currentTimeMillis()))) continue;

            if (candidate.after(Timestamp.valueOf(java.time.LocalDateTime.now().plusMonths(1)))) continue;
            
            if (!OpeningHoursDAO.isOpenForReservation(candidate, 120)) continue;

            if (ReservationDAO.canFitAtTime(candidate, numCustomers)) {
                alternatives.add(candidate);
            }
            if (alternatives.size() == 5) break;
        }
        return alternatives;
    }
}