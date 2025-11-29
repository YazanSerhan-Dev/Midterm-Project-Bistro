package Server;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

public class BistroServer extends AbstractServer {

    private ServerController controller;
    private ReservationDAO reservationDAO = new ReservationDAO();

    public BistroServer(int port, ServerController controller) {
        super(port);
        this.controller = controller;
    }

    private void log(String msg) {
        if (controller != null) {
            controller.appendLogFromServer(msg);
        } else {
            System.out.print(msg);
        }
    }

    @Override
    protected void serverStarted() {
        log("OCSF server started on port " + getPort() + "\n");
    }

    @Override
    protected void serverStopped() {
        log("OCSF server stopped.\n");
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        log("Client connected: " + client.getInetAddress() + "\n");
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        String text = String.valueOf(msg);
        log("Received from client: " + text + "\n");

        try {
            if ("GET_RESERVATIONS".equals(text)) {
                handleGetReservations(client);
            } else if (text.startsWith("UPDATE_RESERVATION:")) {
                handleUpdateReservation(text, client);
            } else {
                client.sendToClient("ERROR: Unknown command");
            }
        } catch (Exception e) {
            log("Error handling client message: " + e.getMessage() + "\n");
            e.printStackTrace();
            try {
                client.sendToClient("ERROR: " + e.getMessage());
            } catch (IOException ignored) {}
        }
    }

    private void handleGetReservations(ConnectionToClient client)
            throws SQLException, IOException {

        List<Reservation> reservations = reservationDAO.getAllReservations();

        if (reservations.isEmpty()) {
            client.sendToClient("NO_RESERVATIONS");
            log("Sent NO_RESERVATIONS\n");
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

        String reply = sb.toString().trim();
        client.sendToClient(reply);
        log("Sent " + reservations.size() + " reservations\n");
    }

    private void handleUpdateReservation(String text, ConnectionToClient client)
            throws SQLException, IOException {

        // פורמט: UPDATE_RESERVATION:<num>:<yyyy-MM-dd>:<guests>
        String[] parts = text.split(":");
        if (parts.length != 4) {
            client.sendToClient("ERROR: Bad UPDATE format");
            return;
        }

        int num = Integer.parseInt(parts[1]);
        String dateStr = parts[2];
        int guests = Integer.parseInt(parts[3]);

        Date newDate = Date.valueOf(dateStr); // yyyy-MM-dd

        reservationDAO.updateReservation(num, newDate, guests);
        client.sendToClient("UPDATE_OK");
        log("Updated reservation #" + num + " to date=" + newDate +
            ", guests=" + guests + "\n");
    }
}

