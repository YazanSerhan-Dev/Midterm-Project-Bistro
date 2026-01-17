package common;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;


/**
 * A generic network wrapper used for all client-server communication.
 *
 * <p>
 * {@code Envelope} standardizes the structure of messages exchanged between
 * the client and the server. Every request and response is wrapped inside
 * an {@code Envelope}, allowing consistent handling, logging, and validation.
 * </p>
 *
 * <p>
 * The {@code payload} field may contain:
 * <ul>
 *   <li>A single DTO</li>
 *   <li>A list of DTOs</li>
 *   <li>A primitive wrapper or String</li>
 * </ul>
 * depending on the {@link OpCode}.
 * </p>
 *
 * <p>
 * Each envelope is automatically assigned a unique request ID and timestamp
 * upon creation, which can be used to match responses to requests.
 * </p>
 */
public class Envelope implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Unique identifier used to correlate requests and responses */
    private String requestId;     // helps match response to request
    /** Time when the envelope was created */
    private Instant timestamp;
    /** Operation code describing the requested or responded action */
    private OpCode op;
    /** Payload data (DTO, list of DTOs, String, etc.) */
    private Object payload;      
    /** Indicates whether the operation succeeded (mainly for responses) */
    private boolean ok;          
    /** Optional informational or error message */
    private String message;      
    /**
     * Default constructor.
     *
     * Automatically generates a unique request ID and assigns
     * the current timestamp.
     */
    public Envelope() {
        this.requestId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }
    /**
     * Creates an envelope with the specified operation code.
     *
     * @param op the operation code for this envelope
     */
    public Envelope(OpCode op) {
        this();
        this.op = op;
    }
    /**
     * Creates a request envelope with a payload.
     *
     * @param op the operation code
     * @param payload the request payload
     * @return a configured request envelope
     */
    public static Envelope request(OpCode op, Object payload) {
        Envelope e = new Envelope(op);
        e.payload = payload;
        e.ok = true;
        return e;
    }
    /**
     * Creates a successful response envelope.
     *
     * @param op the operation code
     * @param payload the response payload
     * @return a successful response envelope
     */
    public static Envelope ok(OpCode op, Object payload) {
        Envelope e = new Envelope(op);
        e.payload = payload;
        e.ok = true;
        return e;
    }
    /**
     * Creates an error envelope with a message.
     *
     * @param msg error description
     * @return an error envelope
     */
    public static Envelope error(String msg) {
        Envelope e = new Envelope(OpCode.ERROR);
        e.ok = false;
        e.message = msg;
        return e;
    }

    // ===== getters/setters =====
    /** @return the request identifier */
    public String getRequestId() { return requestId; }
    /** @param requestId the request identifier to set */
    public void setRequestId(String requestId) { this.requestId = requestId; }

    /** @return the envelope creation timestamp */
    public Instant getTimestamp() { return timestamp; }
    /** @param timestamp the timestamp to set */
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    /** @return the operation code */
    public OpCode getOp() { return op; }
    /** @param op the operation code to set */
    public void setOp(OpCode op) { this.op = op; }

    /** @return the payload object */
    public Object getPayload() { return payload; }
    /** @param payload the payload to set */
    public void setPayload(Object payload) { this.payload = payload; }

    /** @return {@code true} if the operation succeeded */
    public boolean isOk() { return ok; }
    /** @param ok success flag */
    public void setOk(boolean ok) { this.ok = ok; }

    /** @return optional message (info or error) */
    public String getMessage() { return message; }
    /** @param message informational or error message */
    public void setMessage(String message) { this.message = message; }

    /**
     * Returns a concise string representation of the envelope,
     * mainly for debugging purposes.
     *
     * @return string representation of this envelope
     */
    @Override
    public String toString() {
        return "Envelope{op=" + op + ", ok=" + ok + ", msg=" + message + "}";
    }
}
