package Server;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import DataBase.Reservation;
import DataBase.ReservationDAO;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import common.Message;

/**
 * OCSF server for the Bistro system.
 * Listens for client connections and communicates with the DB via ReservationDAO.
 */
public class BistroServer extends AbstractServer {

    /** Reference to the JavaFX server controller (for logging and UI updates). */
    private final ServerController controller;

    /** DAO layer for reservation operations (internally uses the connection pool). */
    private final ReservationDAO reservationDAO = new ReservationDAO();

    /**
     * Creates a new Bistro server on the given port bound to a UI controller.
     */
    public BistroServer(int port, ServerController controller) {
        super(port);
        this.controller = controller;
    }

    /**
     * Logs messages either to the controller log area or to the console.
     */
    private void log(String text) {
        if (controller != null) {
            controller.appendLogFromServer(text);
        } else {
            System.out.println(text);
        }
    }

    /* ========== OCSF lifecycle ========== */

    /**
     * Called when the server starts listening on the port.
     * Notifies the controller to update server status in the UI.
     */
    @Override
    protected void serverStarted() {
        log("Server started on port " + getPort());
        if (controller != null) {
            controller.onServerStarted(getPort());
        }
    }

    /**
     * Called when the server stops listening.
     * Notifies the controller to update server status and clear client table if needed.
     */
    @Override
    protected void serverStopped() {
        log("Server stopped.");
        if (controller != null) {
            controller.onServerStopped();
        }
    }

    /**
     * Called when a new client connects.
     * Logs and adds the client to the server connections table.
     */
    @Override
    protected void clientConnected(ConnectionToClient client) {
        super.clientConnected(client);

        String host = client.getInetAddress().getHostName();
        String ip   = client.getInetAddress().getHostAddress();

        log("Client connected: " + host + " (" + ip + ")");
        if (controller != null) {
            controller.onClientConnected(host, ip);
        }
    }

    /**
     * Called when OCSF detects that a client disconnected cleanly.
     * We only use it to update the UI (status becomes Disconnected).
     */
    @Override
    protected synchronized void clientDisconnected(ConnectionToClient client) {
        log("clientDisconnected event from OCSF");

        if (controller != null) {
            controller.onClientDisconnected(null, null);
        }

        super.clientDisconnected(client);
    }

    /**
     * Called when a client connection ends due to an exception (e.g. closing window).
     * Treated as a disconnect and reflected in the UI.
     */
    @Override
    protected void clientException(ConnectionToClient client, Throwable exception) {
        log("Client disconnected (exception): " +
                (exception != null && exception.getMessage() != null
                        ? exception.getMessage()
                        : "connection closed"));

        if (controller != null) {
            controller.onClientDisconnected(null, null);
        }

        super.clientException(client, exception);
    }

    /* ========== Handle messages from client ========== */

    /**
     * Main dispatcher for messages coming from clients.
     * Validates message type and routes to the correct handler.
     */
    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        log("Received from client: " + msg);

        try {
            if (!(msg instanceof Message)) {
                client.sendToClient(new Message(
                        Message.Type.ERROR,
                        "Unsupported message object from client"
                ));
                return;
            }

            Message m = (Message) msg;

            switch (m.getType()) {
                case GET_RESERVATIONS:
                    handleGetReservations(client);
                    break;

                case UPDATE_RESERVATION:
                    handleUpdateReservation(m, client);
                    break;

                default:
                    client.sendToClient(new Message(
                            Message.Type.ERROR,
                            "Unknown message type: " + m.getType()
                    ));
            }

        } catch (Exception e) {
            log("Error handling client message: " + e.getMessage());
            e.printStackTrace();

            try {
                client.sendToClient(new Message(
                        Message.Type.ERROR,
                        "Server exception: " + e.getMessage()
                ));
            } catch (IOException ignored) {
                log("Failed to send error message to client: " + ignored.getMessage());
            }
        }
    }

    /* ========== Commands ========== */

    /**
     * Handles GET_RESERVATIONS:
     * loads all reservations from the DB and sends them as text to the client.
     */
    private void handleGetReservations(ConnectionToClient client)
            throws IOException, SQLException {

        List<Reservation> reservations = reservationDAO.getAllReservations();

        if (reservations.isEmpty()) {
            client.sendToClient(new Message(
                    Message.Type.RESERVATIONS_TEXT,
                    "NO_RESERVATIONS"
            ));
            log("No reservations found");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Reservation r : reservations) {
            sb.append(r.getReservationNumber())
              .append(" | date=")
              .append(r.getReservationDate())
              .append(" | guests=")
              .append(r.getNumberOfGuests())
              .append("\n");
        }

        client.sendToClient(new Message(
                Message.Type.RESERVATIONS_TEXT,
                sb.toString()
        ));
        log("Sent " + reservations.size() + " reservations to client");
    }

    /**
     * Handles UPDATE_RESERVATION:
     * validates message fields, updates DB, and sends result back to the client.
     */
    private void handleUpdateReservation(Message m, ConnectionToClient client)
            throws IOException, SQLException {

        Integer num     = m.getReservationNumber();
        String dateStr  = m.getReservationDate();
        Integer guests  = m.getNumberOfGuests();

        if (num == null || dateStr == null || guests == null) {
            Message error = new Message(
                    Message.Type.UPDATE_RESULT,
                    "Missing fields for update"
            );
            error.setSuccess(false);
            client.sendToClient(error);
            return;
        }

        Date newDate = Date.valueOf(dateStr); // yyyy-MM-dd

        reservationDAO.updateReservation(num, newDate, guests);

        Message ok = new Message(
                Message.Type.UPDATE_RESULT,
                "UPDATE_OK"
        );
        ok.setSuccess(true);
        client.sendToClient(ok);

        log("Updated reservation #" + num + " to date=" + newDate +
                ", guests=" + guests);
    }
}




