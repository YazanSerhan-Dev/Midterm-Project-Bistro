package Server;

import org.eclipse.paho.client.mqttv3.*;
import java.nio.charset.StandardCharsets;

public class MqttBridge {
    private final String brokerUrl;
    private final String clientId;
    private MqttClient client;

    public MqttBridge(String brokerUrl, String clientId) {
        this.brokerUrl = brokerUrl;
        this.clientId = clientId;
    }

    public void connect() throws MqttException {
        client = new MqttClient(brokerUrl, clientId);
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);
        opts.setConnectionTimeout(5);
        opts.setKeepAliveInterval(20);
        client.connect(opts);
        System.out.println("[MQTT] Connected to " + brokerUrl);
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void publishRetained(String topic, String payloadJson) throws MqttException {
        if (!isConnected()) return;
        MqttMessage msg = new MqttMessage(payloadJson.getBytes(StandardCharsets.UTF_8));
        msg.setQos(1);
        msg.setRetained(true);
        client.publish(topic, msg);
    }
}
