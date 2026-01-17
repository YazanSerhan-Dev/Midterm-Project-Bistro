package common;

import java.io.Serializable;
/**
 * A lightweight transport wrapper used to send Kryo-serialized data
 * between client and server.
 *
 * <p>
 * {@code KryoMessage} encapsulates a raw byte payload produced by
 * {@link KryoUtil} together with a simple string {@code type} identifier.
 * It is used as the low-level network message that actually travels
 * over the socket.
 * </p>
 *
 * <p>
 * In this project, {@code KryoMessage} is typically used to wrap an
 * {@link Envelope} instance after it has been serialized to bytes.
 * The {@code type} field allows quick identification of the message
 * category before deserialization.
 * </p>
 *
 * <p>
 * This class is intentionally minimal and immutable to ensure
 * safe transport across the network.
 * </p>
 */
public class KryoMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Logical message type identifier (e.g. "ENVELOPE") */
    private final String type;     // e.g. "GET_MY_RESERVATIONS"
    
    /** Kryo-serialized byte payload */
    private final byte[] payload;  // Kryo bytes

    /**
     * Creates a new KryoMessage with the given type and serialized payload.
     *
     * @param type logical message type identifier
     * @param payload Kryo-serialized byte array
     */
    public KryoMessage(String type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }
    /**
     * Returns the message type identifier.
     *
     * @return message type
     */
    public String getType() { return type; }
    /**
     * Returns the raw Kryo-serialized payload.
     *
     * @return payload as byte array
     */
    public byte[] getPayload() { return payload; }

    /**
     * Convenience factory method that serializes an object
     * into Kryo bytes and wraps it in a {@code KryoMessage}.
     *
     * @param type logical message type identifier
     * @param obj object to serialize
     * @return a new {@code KryoMessage} instance
     */
    public static KryoMessage of(String type, Object obj) {
        return new KryoMessage(type, KryoUtil.toBytes(obj));
    }
    /**
     * Deserializes the payload back into an object.
     *
     * <p>
     * The caller is responsible for knowing the expected
     * object type.
     * </p>
     *
     * @param <T> expected object type
     * @return deserialized object
     */
    public <T> T payloadObject() {
        return KryoUtil.fromBytes(payload);
    }
}
