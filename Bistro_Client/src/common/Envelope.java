package common;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * One wrapper that always travels between client/server.
 * payload can be a DTO (or List<DTO>) depending on the OpCode.
 */
public class Envelope implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;     // helps match response to request
    private Instant timestamp;
    private OpCode op;
    private Object payload;       // DTO / List<DTO> / String / etc.
    private boolean ok;           // for responses
    private String message;       // info/error text

    public Envelope() {
        this.requestId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public Envelope(OpCode op) {
        this();
        this.op = op;
    }

    public static Envelope request(OpCode op, Object payload) {
        Envelope e = new Envelope(op);
        e.payload = payload;
        e.ok = true;
        return e;
    }

    public static Envelope ok(OpCode op, Object payload) {
        Envelope e = new Envelope(op);
        e.payload = payload;
        e.ok = true;
        return e;
    }

    public static Envelope error(String msg) {
        Envelope e = new Envelope(OpCode.ERROR);
        e.ok = false;
        e.message = msg;
        return e;
    }

    // ===== getters/setters =====
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public OpCode getOp() { return op; }
    public void setOp(OpCode op) { this.op = op; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "Envelope{op=" + op + ", ok=" + ok + ", msg=" + message + "}";
    }
}
