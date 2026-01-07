package Server;

import java.io.IOException;
import java.sql.Date; // From HEAD
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
import common.dto.TerminalValidateResponseDTO;
import common.dto.WaitingListDTO;
// Imports from MAIN (Reservation & Login)
import common.dto.LoginResponseDTO;
import common.dto.MakeReservationRequestDTO;
import common.dto.MakeReservationResponseDTO;
import common.dto.ProfileDTO;
import DataBase.Reservation;
import DataBase.dao.BillDAO;
import DataBase.dao.ReservationDAO;
import DataBase.dao.SubscriberDAO;
import DataBase.dao.UserActivityDAO;
import DataBase.dao.WaitingListDAO;
import DataBase.dao.RestaurantTableDAO; // From MAIN

public class BistroServer extends AbstractServer {

    private final ServerController controller;
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final SubscriberDAO subscriberDAO = new SubscriberDAO();

    public BistroServer(int port, ServerController controller) {
        super(port);
        this.controller = controller;
    }

    private void log(String text) {
        if (controller != null) controller.appendLogFromServer(text);
        else System.out.println(text);
    }

    private String host(ConnectionToClient c) {
        return (c.getInetAddress() != null) ? c.getInetAddress().getHostName() : "unknown";
    }

    private String ip(ConnectionToClient c) {
        return (c.getInetAddress() != null) ? c.getInetAddress().getHostAddress() : "unknown";
    }

    @Override
    protected void serverStarted() {
        log("Server started on port " + getPort());
        if (controller != null) controller.onServerStarted(getPort());
        BackgroundJobs.start();
    }

    @Override
    protected void serverStopped() {
        log("Server stopped.");
        if (controller != null) controller.onServerStopped();
        BackgroundJobs.stop();
    }

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


    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        try {
            Envelope req = unwrapToEnvelope(msg);
            if (req == null) {
                sendError(client, OpCode.ERROR, "Bad message format (expected Envelope via KryoMessage).");
                return;
            }

            switch (req.getOp()) {
                case REQUEST_RESERVATIONS_LIST -> handleReservationsList(req, client);
                case REQUEST_REGISTER_CUSTOMER -> handleRegisterCustomer(req, client);
                case REQUEST_SUBSCRIBERS_LIST -> handleSubscribersList(client);
                case REQUEST_AGENT_RESERVATIONS_LIST -> handleAgentReservationsList(client);
                
                case REQUEST_MAKE_RESERVATION -> handleMakeReservation(req, client);
                case REQUEST_CHECK_AVAILABILITY -> handleCheckAvailability(req, client);
                
                case REQUEST_LOGIN_SUBSCRIBER -> handleLoginSubscriber(req, client);
                case REQUEST_LOGIN_STAFF      -> handleLoginStaff(req, client);
                
                case REQUEST_TERMINAL_VALIDATE_CODE -> handleTerminalValidateCode(req, client);
                case REQUEST_TERMINAL_CHECK_IN -> handleTerminalCheckIn(req, client);
                
                //updated from Yazan for the waiting list
                case REQUEST_WAITING_LIST -> handleWaitingList(req, client);
                
                case REQUEST_LEAVE_WAITING_LIST -> handleLeaveWaitingList(req, client);

                // TODO later:
                case REQUEST_CANCEL_RESERVATION -> handleCancelReservation(req, client);
                case REQUEST_HISTORY_GET -> sendOk(client, OpCode.RESPONSE_HISTORY_GET, List.of());
                case REQUEST_BILL_GET_BY_CODE -> handleBillGetByCode(req, client);
                case REQUEST_PAY_BILL        -> handlePayBill(req, client);
                case REQUEST_TERMINAL_CHECK_OUT -> sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_OUT, "NOT_IMPLEMENTED_YET");
                case REQUEST_TERMINAL_NO_SHOW -> sendOk(client, OpCode.RESPONSE_TERMINAL_NO_SHOW, "NOT_IMPLEMENTED_YET");
                
                case REQUEST_GET_PROFILE -> handleGetProfile(req, client);
                case REQUEST_UPDATE_PROFILE -> handleUpdateProfile(req, client);

                default -> sendError(client, OpCode.ERROR, "Unknown op: " + req.getOp());
            }

        } catch (Exception e) {
            log("Error handling message: " + e.getMessage());
            try {
                sendError(client, OpCode.ERROR, "Server exception: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }

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

    private void sendOk(ConnectionToClient client, OpCode op, Object payload) throws IOException {
        Envelope resp = Envelope.ok(op, payload);
        sendEnvelope(client, resp);
    }

    private void sendError(ConnectionToClient client, OpCode op, String message) throws IOException {
        Envelope resp = Envelope.error(message);
        resp.setOp(op);
        sendEnvelope(client, resp);
    }

    private void sendEnvelope(ConnectionToClient client, Envelope env) throws IOException {
        byte[] bytes = KryoUtil.toBytes(env);
        client.sendToClient(new KryoMessage("ENVELOPE", bytes));
    }

    /* ==================== Handlers ==================== */
    
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

    // small helper for null-safety
    private boolean see(boolean b) { return b; }

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

            System.out.println("[LEAVE_WAITING_LIST] code=" + code + " role=" + role + " username=" + username);

            if (code.isBlank()) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Missing waiting code.");
                return;
            }
            
            System.out.println("[LEAVE_WAITING_LIST] passed code checks");

            // (Optional) if SUBSCRIBER you can require username, same style as handleWaitingList
            if ("SUBSCRIBER".equalsIgnoreCase(role) && username.isBlank()) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Missing subscriber username.");
                return;
            }

            // Load entry by code
            WaitingListDTO w = WaitingListDAO.getByCode(code);
            System.out.println("[LEAVE_WAITING_LIST] w=" + (w == null ? "null" : ("status=" + w.getStatus())));
            
            if (w == null) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Waiting code not found.");
                return;
            }

            String st = w.getStatus() == null ? "" : w.getStatus().trim();

            // Business rules:
            if ("CANCELED".equalsIgnoreCase(st)) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Already canceled.");
                return;
            }
            if ("ASSIGNED".equalsIgnoreCase(st)) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Already assigned a table. Can't leave now.");
                return;
            }
            if (!"WAITING".equalsIgnoreCase(st)) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Invalid status: " + st);
                return;
            }

            // Update status -> CANCELED (only if WAITING)
            System.out.println("[LEAVE_WAITING_LIST] about to cancel in DB");
            boolean ok = WaitingListDAO.cancelIfWaitingByCode(code);
            System.out.println("[LEAVE_WAITING_LIST] updated=" + ok);

            if (!ok) {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Cancel failed (already changed).");
                return;
            }

            // Return updated DTO
            w.setStatus("CANCELED");
            sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, w);

        } catch (Exception e) {
            try {
                sendOk(client, OpCode.RESPONSE_LEAVE_WAITING_LIST, "Server error: " + e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    
    private void handleWaitingList(Envelope req, ConnectionToClient client) {
        try {
            Object payload = readEnvelopePayload(req);

            if (!(payload instanceof Object[] arr) || arr.length < 3 || !(arr[2] instanceof WaitingListDTO dto)) {
                sendOk(client, OpCode.RESPONSE_WAITING_LIST, "Bad payload.");
                return;
            }

            String role = arr[0] == null ? "" : arr[0].toString();
            String username = arr[1] == null ? "" : arr[1].toString();

            int people = dto.getPeopleCount();
            if (people <= 0) {
                sendOk(client, OpCode.RESPONSE_WAITING_LIST, "People count must be > 0.");
                return;
            }
            int maxSeats = RestaurantTableDAO.getMaxTableSeats();

            if (people > maxSeats) {
                String msg = "No table can accommodate " + people
                        + " guests. Maximum table size is " + maxSeats
                        + ". Please split into two parties or contact staff.";

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
                // CUSTOMER: must send email + phone
                email = dto.getEmail() == null ? "" : dto.getEmail().trim();
                phone = dto.getPhone() == null ? "" : dto.getPhone().trim();

                if (email.isBlank() || phone.isBlank()) {
                    sendOk(client, OpCode.RESPONSE_WAITING_LIST, "Email and phone are required for customers.");
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

            // ✅ store contact info in user_activity for later email notification
            UserActivityDAO.insertWaitingActivity(waitingId, email, phone);

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


    // -----------------------------------------------------------
    // 1. REGISTRATION (From your HEAD branch)
    // -----------------------------------------------------------
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
                dto.getMemberCode(),
                dto.getBarcode(),
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
            if (dto.getGuestEmail() == null || dto.getGuestEmail().isBlank()) {
                sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION, new MakeReservationResponseDTO(false, -1, null, "Guest email is required."));
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

        Timestamp start = dto.getReservationTime();
        Timestamp expiry = Timestamp.valueOf(dto.getReservationTime().toLocalDateTime().plusMinutes(15)); 

        int totalSeats = DataBase.dao.RestaurantTableDAO.getTotalSeatsAvailable();
        int bookedCustomers = DataBase.dao.ReservationDAO.getBookedCustomersInRange(start, expiry);

        if (bookedCustomers + dto.getNumOfCustomers() > totalSeats) {
            List<Timestamp> suggestions = findAlternativeTimes(dto.getReservationTime(), dto.getNumOfCustomers());
            sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION, new MakeReservationResponseDTO(false, "No available seats at requested time.", suggestions));
            return;
        }

        // Create reservation
        ReservationDAO.CreateReservationResult r = reservationDAO.createReservationWithActivity(dto);

        // Send Email (Optional, wrapped in try/catch so it doesn't crash server)
        try {
            String toEmail;
            if (dto.isSubscriber()) {
                toEmail = DataBase.dao.SubscriberDAO.getEmailByUsername(dto.getSubscriberUsername());
            } else {
                toEmail = dto.getGuestEmail();
            }
            if (toEmail != null && !toEmail.isBlank()) {
                Server.EmailService.sendReservationConfirmation(toEmail, r.confirmationCode);
            }
        } catch (Exception mailEx) {
            System.out.println("[EMAIL] Failed to send email: " + mailEx.getMessage());
        }

        sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION, new MakeReservationResponseDTO(true, r.reservationId, r.confirmationCode, "Reservation created successfully!"));
    }

    // -----------------------------------------------------------
    // 3. OTHER HANDLERS
    // -----------------------------------------------------------

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

        Timestamp start = dto.getReservationTime();
        Timestamp expiry = Timestamp.valueOf(start.toLocalDateTime().plusMinutes(15));

        int totalSeats = RestaurantTableDAO.getTotalSeatsAvailable();
        int booked = ReservationDAO.getBookedCustomersInRange(start, expiry);

        if (booked + dto.getNumOfCustomers() > totalSeats) {
            List<Timestamp> suggestions = findAlternativeTimes(dto.getReservationTime(), dto.getNumOfCustomers());
            sendOk(client, OpCode.RESPONSE_CHECK_AVAILABILITY, new MakeReservationResponseDTO(false, "No available seats at requested time.", suggestions));
            return;
        }

        sendOk(client, OpCode.RESPONSE_CHECK_AVAILABILITY, new MakeReservationResponseDTO(true, -1, null, "Available"));
    }

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

            // 1) If this is a RESERVATION code -> use existing behavior
            TerminalValidateResponseDTO info = DataBase.dao.ReservationDAO.getTerminalInfoByCode(code);
            if (info != null && info.isValid()) {
                String tableId = DataBase.dao.ReservationDAO.markArrivedByCodeReturnTableId(code);

                TerminalValidateResponseDTO dto = DataBase.dao.ReservationDAO.getTerminalInfoByCode(code);
                dto.setValid(true);
                dto.setStatus("ARRIVED");
                dto.setMessage("ARRIVED");
                dto.setTableId(tableId);

                sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_IN, dto);
                return;
            }

            // 2) Otherwise, try WAITING LIST code
            WaitingListDTO w = WaitingListDAO.getByCode(code);
            if (w == null) {
                sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_IN,
                        new TerminalValidateResponseDTO(false, "Code not found."));
                return;
            }

            String st = w.getStatus() == null ? "" : w.getStatus().trim();

            // only ASSIGNED can check-in
            if (!"ASSIGNED".equalsIgnoreCase(st)) {
                TerminalValidateResponseDTO dto = new TerminalValidateResponseDTO();
                dto.setValid(true);
                dto.setStatus(st.isBlank() ? "WAITING" : st);
                dto.setNumOfCustomers(w.getPeopleCount());
                dto.setCheckInAllowed(false);
                dto.setMessage("Still waiting. Check-in allowed only when status is ASSIGNED.");
                dto.setTableId("-");
                sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_IN, dto);
                return;
            }

            // consume / close the waiting entry (no “entered” status in your enum)
            WaitingListDAO.updateStatusByCode(code, "CANCELED");

            TerminalValidateResponseDTO dto = new TerminalValidateResponseDTO();
            dto.setValid(true);
            dto.setStatus("ASSIGNED");
            dto.setNumOfCustomers(w.getPeopleCount());
            dto.setCheckInAllowed(false);
            dto.setMessage("Checked-in successfully (from waiting list).");
            dto.setTableId("-");

            sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_IN, dto);

        } catch (Exception e) {
            TerminalValidateResponseDTO err = new TerminalValidateResponseDTO();
            err.setValid(false);
            err.setMessage(e.getMessage());
            try {
                sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_IN, err);
            } catch (Exception ignored) {}
        }
    }


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


    private void handleLoginStaff(Envelope env, ConnectionToClient client) {
        try {
            var req = (common.dto.LoginRequestDTO) env.getPayload();
            String role = DataBase.dao.StaffDAO.checkLoginAndGetRole(req.getUsername(), req.getPassword());
            LoginResponseDTO res = (role != null)
                    ? new common.dto.LoginResponseDTO(true, "Login success", req.getUsername(), null, role)
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

    private void handleSubscribersList(ConnectionToClient client) {
        try {
            List<SubscriberDTO> list = DataBase.dao.SubscriberDAO.getAllSubscribers();
            sendOk(client, OpCode.RESPONSE_SUBSCRIBERS_LIST, list);
            log("Sent " + list.size() + " subscribers to client.");
        } catch (Exception e) {
            log("Error fetching subscribers: " + e.getMessage());
            try { sendError(client, OpCode.ERROR, "Fetch failed"); } catch (Exception ignored) {}
        }
    }

    private void handleAgentReservationsList(ConnectionToClient client) {
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

    /* ==================== Helpers ==================== */

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

    private void invoke(Object obj, String method, Class<?> param, Object value) {
        try {
            obj.getClass().getMethod(method, param).invoke(obj, value);
        } catch (Exception ignored) {}
    }

    private Object readEnvelopePayload(Envelope env) {
        Object val = tryInvoke(env, "getPayload");
        if (val != null) return val;
        val = tryInvoke(env, "getData");
        if (val != null) return val;
        val = tryInvoke(env, "getBody");
        return val;
    }

    private Object tryInvoke(Object target, String methodName) {
        try {
            var m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<Timestamp> findAlternativeTimes(Timestamp requested, int numCustomers) throws Exception {
        List<Timestamp> alternatives = new ArrayList<>();
        int[] offsets = { -90, -60, -30, 30, 60, 90 };

        for (int minutes : offsets) {
            Timestamp candidate = Timestamp.valueOf(requested.toLocalDateTime().plusMinutes(minutes));
            
            int m = candidate.toLocalDateTime().getMinute();
            if (m != 0 && m != 30) continue;

            if (candidate.after(Timestamp.valueOf(java.time.LocalDateTime.now().plusMonths(1)))) continue;

            if (ReservationDAO.canFitAtTime(candidate, numCustomers)) {
                alternatives.add(candidate);
            }
            if (alternatives.size() == 5) break;
        }
        return alternatives;
    }
}