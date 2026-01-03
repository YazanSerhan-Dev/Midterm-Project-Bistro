package Server;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

import common.Envelope;
import common.KryoMessage;
import common.KryoUtil;
import common.OpCode;
import common.dto.LoginResponseDTO;
import common.dto.MakeReservationRequestDTO;
import common.dto.MakeReservationResponseDTO;

import DataBase.Reservation;
import DataBase.dao.ReservationDAO;

public class BistroServer extends AbstractServer {

    private final ServerController controller;
    private final ReservationDAO reservationDAO = new ReservationDAO();

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
    }

    @Override
    protected void serverStopped() {
        log("Server stopped.");
        if (controller != null) controller.onServerStopped();
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        super.clientConnected(client);

        String host = host(client);
        String ip = ip(client);

        log("Client connected: " + host + " (" + ip + ")");

        // create + remember a "row key" for this exact client connection
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

        // IMPORTANT: exceptions often happen instead of clean disconnect
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

                // TODO later:
                case REQUEST_CANCEL_RESERVATION -> sendOk(client, OpCode.RESPONSE_CANCEL_RESERVATION, "NOT_IMPLEMENTED_YET");
                case REQUEST_RECOVER_CONFIRMATION_CODE -> sendOk(client, OpCode.RESPONSE_RECOVER_CONFIRMATION_CODE, "NOT_IMPLEMENTED_YET");
                case REQUEST_PROFILE_GET -> sendOk(client, OpCode.RESPONSE_PROFILE_GET, null);
                case REQUEST_PROFILE_UPDATE_CONTACT -> sendOk(client, OpCode.RESPONSE_PROFILE_UPDATE_CONTACT, "NOT_IMPLEMENTED_YET");
                case REQUEST_HISTORY_GET -> sendOk(client, OpCode.RESPONSE_HISTORY_GET, List.of());

                case REQUEST_BILL_GET_BY_CODE -> sendOk(client, OpCode.RESPONSE_BILL_GET_BY_CODE, null);
                case REQUEST_PAY_BILL -> sendOk(client, OpCode.RESPONSE_PAY_BILL, "NOT_IMPLEMENTED_YET");

                case REQUEST_TERMINAL_VALIDATE_CODE -> sendOk(client, OpCode.RESPONSE_TERMINAL_VALIDATE_CODE, "NOT_IMPLEMENTED_YET");
                case REQUEST_TERMINAL_CHECK_IN -> sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_IN, "NOT_IMPLEMENTED_YET");
                case REQUEST_TERMINAL_CHECK_OUT -> sendOk(client, OpCode.RESPONSE_TERMINAL_CHECK_OUT, "NOT_IMPLEMENTED_YET");
                case REQUEST_TERMINAL_NO_SHOW -> sendOk(client, OpCode.RESPONSE_TERMINAL_NO_SHOW, "NOT_IMPLEMENTED_YET");
                
                case REQUEST_LOGIN_SUBSCRIBER -> handleLoginSubscriber(req, client);
                case REQUEST_LOGIN_STAFF      -> handleLoginStaff(req, client);

                
                case REQUEST_MAKE_RESERVATION -> handleMakeReservation(req, client);

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

    private void handleLoginSubscriber(Envelope env, ConnectionToClient client) {
        try {
            var req = (common.dto.LoginRequestDTO) env.getPayload();

            boolean ok = DataBase.dao.SubscriberDAO.checkLogin(req.getUsername(), req.getPassword());

            LoginResponseDTO res = ok
                    ? new common.dto.LoginResponseDTO(true, "Login success", req.getUsername(), null, "SUBSCRIBER")
                    : new common.dto.LoginResponseDTO(false, "Invalid username or password", null, null, null);

            Envelope reply = Envelope.ok(OpCode.RESPONSE_LOGIN_SUBSCRIBER, res);
            client.sendToClient(new KryoMessage("ENVELOPE", KryoUtil.toBytes(reply)));

        } catch (Exception e) {
            try {
                LoginResponseDTO res =
                        new common.dto.LoginResponseDTO(false, "Server error: " + e.getMessage(), null, null, null);
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
                LoginResponseDTO res =
                        new common.dto.LoginResponseDTO(false, "Server error: " + e.getMessage(), null, null, null);
                Envelope reply = Envelope.ok(OpCode.RESPONSE_LOGIN_STAFF, res);
                client.sendToClient(new KryoMessage("ENVELOPE", KryoUtil.toBytes(reply)));
            } catch (Exception ignored) {}
        }
    }

    
    private void handleReservationsList(Envelope req, ConnectionToClient client) throws Exception {
        List<Reservation> rows = reservationDAO.getAllReservations(); // must match your DAO method name

        List<Object> dtoList = new ArrayList<>();
        for (Reservation r : rows) {
            Object dto = toReservationDTO(r);
            if (dto != null) dtoList.add(dto);
        }

        sendOk(client, OpCode.RESPONSE_RESERVATIONS_LIST, dtoList);
        log("Sent RESPONSE_RESERVATIONS_LIST size=" + dtoList.size());
    }

    /**
     * Maps DB Reservation -> common.dto.ReservationDTO
     * using reflection so server won’t break if DTO changes a bit.
     */
    private Object toReservationDTO(Reservation r) {
        try {
            Class<?> dtoCls = Class.forName("common.dto.ReservationDTO");
            Object dto = dtoCls.getDeclaredConstructor().newInstance();

            // DB values
            int reservationId = r.getReservationId();
            String confirmationCode = r.getConfirmationCode();
            Timestamp resTs = r.getReservationTime();
            Timestamp expTs = r.getExpiryTime();
            int customers = r.getNumOfCustomers();
            String status = r.getStatus();

            String resTimeStr = (resTs == null) ? "-" : resTs.toLocalDateTime().toString().replace('T', ' ');
            String expTimeStr = (expTs == null) ? "-" : expTs.toLocalDateTime().toString().replace('T', ' ');

            // Try multiple setter names (so DTO can be slightly different)
            invoke(dto, "setReservationId", int.class, reservationId);
            invoke(dto, "setReservationId", Integer.class, reservationId);

            invoke(dto, "setConfirmationCode", String.class, confirmationCode);
            invoke(dto, "setCode", String.class, confirmationCode); // fallback if you named it "code"

            invoke(dto, "setReservationTime", String.class, resTimeStr);
            invoke(dto, "setExpiryTime", String.class, expTimeStr);

            invoke(dto, "setNumOfCustomers", int.class, customers);
            invoke(dto, "setNumOfCustomers", Integer.class, customers);

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
    private void handleMakeReservation(Envelope req, ConnectionToClient client) throws Exception {

        // Read payload safely even if Envelope uses different getter name
        Object payloadObj = readEnvelopePayload(req);
        if (!(payloadObj instanceof MakeReservationRequestDTO dto)) {
            sendError(client, OpCode.RESPONSE_MAKE_RESERVATION, "Bad payload: expected MakeReservationRequestDTO");
            return;
        }

        // Basic validation
        if (dto.getNumOfCustomers() <= 0 || dto.getReservationTime() == null) {
            sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION,
                    new MakeReservationResponseDTO(false, -1, null, "Invalid input."));
            return;
        }

        if (!dto.isSubscriber()) {
            if (dto.getGuestPhone() == null || dto.getGuestPhone().isBlank()
                    || dto.getGuestEmail() == null || dto.getGuestEmail().isBlank()) {
                sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION,
                        new MakeReservationResponseDTO(false, -1, null, "Guest phone + email are required."));
                return;
            }
        }

        // Prevent past times (simple rule for now)
        Timestamp nowPlus5 = new Timestamp(System.currentTimeMillis() + 5L * 60L * 1000L);
        if (dto.getReservationTime().before(nowPlus5)) {
            sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION,
                    new MakeReservationResponseDTO(false, -1, null, "Choose a future time (5+ minutes ahead)."));
            return;
        }

        // Create reservation in DB
        ReservationDAO.CreateReservationResult r = reservationDAO.createReservationWithActivity(dto);

        sendOk(client, OpCode.RESPONSE_MAKE_RESERVATION,
                new MakeReservationResponseDTO(true, r.reservationId, r.confirmationCode,
                        "Reservation created successfully!"));
    }
    
    private Object readEnvelopePayload(Envelope env) {
        // Try common getter names without assuming Envelope API
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


}





