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
 * - Listens for client connections
 * - Handles messages from clients
 * - Uses ReservationDAO (and the connection pool) to talk to the DB
 * - Reports log messages back to the ServerController (GUI)
 */
public class BistroServer extends AbstractServer {

    // Reference to the JavaFX controller (for logging in the UI)
    private final ServerController controller;

    // DAO layer for reservations (uses the connection pool internally)
    private final ReservationDAO reservationDAO = new ReservationDAO();


    public BistroServer(int port, ServerController controller) {
        super(port);
        this.controller = controller;
    }

    private void log(String msg) {
        if (controller != null) {
            controller.appendLogFromServer(msg);
        } else {
            System.out.println(msg);
        }
    }

    /* OCSF hooks */

    @Override
    protected void serverStarted() {
        log("Server started on port " + getPort());
        controller.onServerStarted(getPort());
    }

    @Override
    protected void serverStopped() {
        log("Server stopped.");
        controller.onServerStopped();
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        super.clientConnected(client);
        String host = client.getInetAddress().getHostName();
        String ip = client.getInetAddress().getHostAddress();
        controller.onClientConnected(host, ip);
        log("Client connected: " + host + " (" + ip + ")");
    }

    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) {
        super.clientDisconnected(client);

        java.net.InetAddress addr = client.getInetAddress();
        if (addr == null) {
            // כבר סגור לגמרי, רק נרשום ללוג ולא נעשה כלום
            log("clientDisconnected called but InetAddress is null (already closed)\n");
            return;
        }

        String host = addr.getHostName();
        String ip   = addr.getHostAddress();

        controller.onClientDisconnected(host, ip);
        log("Client disconnected: " + host + " (" + ip + ")\n");
    }


    @Override
    protected void clientException(ConnectionToClient client, Throwable exception) {
        clientDisconnected(client);
        log("Client exception: " + exception.getMessage());
    }



    // ==================== Handling client messages ====================

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        log("Received from client: " + msg + "\n");

        try {
            if (msg instanceof Message) {
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

            } else if (msg instanceof String) {
                // optional: support old string protocol if you want backward compatibility
                String text = (String) msg;
                log("Received legacy string message: " + text + "\n");
                client.sendToClient(new Message(
                        Message.Type.ERROR,
                        "Legacy string protocol not supported anymore"
                ));

            } else {
                client.sendToClient(new Message(
                        Message.Type.ERROR,
                        "Unsupported message object: " + msg.getClass().getName()
                ));
            }

        } catch (Exception e) {
            log("Error handling client message: " + e.getMessage() + "\n");
            e.printStackTrace();

            try {
                client.sendToClient(new Message(
                        Message.Type.ERROR,
                        "Server exception: " + e.getMessage()
                ));
            } catch (IOException ioEx) {
                // even sending the error failed – just log it
                log("Failed to send error message to client: " + ioEx.getMessage() + "\n");
                ioEx.printStackTrace();
            }
        }

    }




    /**
     * Handle the "GET_RESERVATIONS" command.
     */
    private void handleGetReservations(ConnectionToClient client) throws IOException, SQLException {
        List<Reservation> reservations = reservationDAO.getAllReservations(); // use your method

        if (reservations.isEmpty()) {
            Message reply = new Message(Message.Type.RESERVATIONS_TEXT, "NO_RESERVATIONS");
            client.sendToClient(reply);
            log("No reservations found\n");
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

        Message reply = new Message(Message.Type.RESERVATIONS_TEXT, sb.toString());
        client.sendToClient(reply);

        log("Sent " + reservations.size() + " reservations\n");
    }


    /**
     * Handle the "UPDATE_RESERVATION" command.
     * Format: UPDATE_RESERVATION:<num>:<yyyy-MM-dd>:<guests>
     */
    private void handleUpdateReservation(Message m, ConnectionToClient client) throws IOException, SQLException {
        Integer num = m.getReservationNumber();
        String dateStr = m.getReservationDate();
        Integer guests = m.getNumberOfGuests();

        if (num == null || dateStr == null || guests == null) {
            Message error = new Message(Message.Type.UPDATE_RESULT, "Missing fields for update");
            error.setSuccess(false);
            client.sendToClient(error);
            return;
        }

        Date newDate = Date.valueOf(dateStr); // yyyy-MM-dd

        // your existing DAO method
        reservationDAO.updateReservation(num, newDate, guests);

        Message ok = new Message(Message.Type.UPDATE_RESULT, "UPDATE_OK");
        ok.setSuccess(true);
        client.sendToClient(ok);

        log("Updated reservation #" + num + " to date=" + newDate +
            ", guests=" + guests + "\n");
    }

}


