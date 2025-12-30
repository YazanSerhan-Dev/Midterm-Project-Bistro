package common;

import java.io.Serializable;

public class KryoMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String type;     // e.g. "GET_MY_RESERVATIONS"
    private final byte[] payload;  // Kryo bytes

    public KryoMessage(String type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() { return type; }
    public byte[] getPayload() { return payload; }

    public static KryoMessage of(String type, Object obj) {
        return new KryoMessage(type, KryoUtil.toBytes(obj));
    }

    public <T> T payloadObject() {
        return KryoUtil.fromBytes(payload);
    }
}

