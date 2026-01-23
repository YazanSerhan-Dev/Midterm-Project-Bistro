package Server;

import DataBase.dao.ReservationDAO;
import DataBase.dao.VisitDAO;
import org.eclipse.paho.client.mqttv3.MqttException;

public class ReservationStatsMqttPublisher {

    private final MqttBridge mqtt;
    private final String topic;
    private final long intervalMs;
    private Thread worker;
    private volatile boolean running = false;

    // last published values to avoid spam
    private int lastToday = Integer.MIN_VALUE;
    private int lastServed = Integer.MIN_VALUE;

    public ReservationStatsMqttPublisher(MqttBridge mqtt, String topic, long intervalMs) {
        this.mqtt = mqtt;
        this.topic = topic;
        this.intervalMs = intervalMs;
    }

    public void start() {
        if (running) return;
        running = true;

        worker = new Thread(() -> {
            while (running) {
                try {
                    // If MQTT disconnected (broker restarted), try reconnect
                    if (!mqtt.isConnected()) {
                        try {
                            mqtt.connect();
                        } catch (Exception ignored) {}
                    }

                    int today = ReservationDAO.countReservationsMadeToday();
                    int served = VisitDAO.countReservationsServedToday();

                    // never publish negatives
                    if (today < 0) today = 0;
                    if (served < 0) served = 0;

                    // publish only if changed (optional)
                    if (today != lastToday || served != lastServed) {
                        lastToday = today;
                        lastServed = served;

                        String payload = "{\"today\":" + today + ",\"served\":" + served + "}";
                        mqtt.publishRetained(topic, payload);

                        System.out.println("[MQTT] Published: " + payload + " -> " + topic);
                    }

                } catch (Exception e) {
                    System.out.println("[MQTT] Publisher error: " + e.getMessage());
                }

                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException ignored) {}
            }
        }, "ReservationStatsMqttPublisher");

        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running = false;
        if (worker != null) worker.interrupt();
    }
}
