package common;

/**
 * Defines all operation codes used in client-server communication.
 *
 * <p>
 * {@code OpCode} is the system-wide "protocol dictionary": every message sent between
 * the client and the server uses one value from this enum to indicate the requested
 * action and the expected response type.
 * </p>
 *
 * <h3>Naming Convention</h3>
 * <ul>
 *   <li><b>REQUEST_*</b> - Sent from client to server to ask for an operation.</li>
 *   <li><b>RESPONSE_*</b> - Sent from server to client as a reply to a request.</li>
 *   <li><b>INFO</b> / <b>ERROR</b> - General messages not tied to a specific request/response pair.</li>
 * </ul>
 *
 * <p>
 * In this project, {@code OpCode} is typically wrapped inside {@link common.Envelope}
 * and transmitted over the network (often serialized using {@code KryoUtil}).
 * Both client and server must share the same enum definition to remain compatible.
 * </p>
 */
public enum OpCode {
    // ===== Common =====
    ERROR,
    INFO,

    // ===== Customer/Subscriber Main Page =====
    REQUEST_RESERVATIONS_LIST,
    RESPONSE_RESERVATIONS_LIST,

    REQUEST_CANCEL_RESERVATION,
    RESPONSE_CANCEL_RESERVATION,

    REQUEST_RECOVER_CONFIRMATION_CODE,
    RESPONSE_RECOVER_CONFIRMATION_CODE,
    
    REQUEST_MAKE_RESERVATION,
    RESPONSE_MAKE_RESERVATION,
    
    REQUEST_GET_PROFILE,
    RESPONSE_GET_PROFILE,
    
    REQUEST_UPDATE_PROFILE,
    RESPONSE_UPDATE_PROFILE,

    // Pre-check reservation availability (no email/phone prompt)
    REQUEST_CHECK_AVAILABILITY,
    RESPONSE_CHECK_AVAILABILITY,
    
    REQUEST_GET_AVAILABLE_TIMES,
    RESPONSE_GET_AVAILABLE_TIMES,

    REQUEST_HISTORY_GET,
    RESPONSE_HISTORY_GET,
    
    REQUEST_LOGIN_SUBSCRIBER,
    RESPONSE_LOGIN_SUBSCRIBER,

    REQUEST_LOGIN_STAFF,
    RESPONSE_LOGIN_STAFF,

    // ===== Pay Bill Page =====
    REQUEST_BILL_GET_BY_CODE,
    RESPONSE_BILL_GET_BY_CODE,

    REQUEST_PAY_BILL,
    RESPONSE_PAY_BILL,

    // ===== Terminal Page =====
    REQUEST_TERMINAL_VALIDATE_CODE,
    RESPONSE_TERMINAL_VALIDATE_CODE,
    
    REQUEST_TERMINAL_RESOLVE_SUBSCRIBER_QR,
    RESPONSE_TERMINAL_RESOLVE_SUBSCRIBER_QR,

    REQUEST_TERMINAL_CHECK_IN,
    RESPONSE_TERMINAL_CHECK_IN,

    REQUEST_TERMINAL_CHECK_OUT,
    RESPONSE_TERMINAL_CHECK_OUT,

    REQUEST_TERMINAL_NO_SHOW,
    RESPONSE_TERMINAL_NO_SHOW,
    
    REQUEST_TERMINAL_CANCEL_RESERVATION,
    RESPONSE_TERMINAL_CANCEL_RESERVATION,
    
    REQUEST_TERMINAL_GET_SUBSCRIBER_ACTIVE_CODES,
    RESPONSE_TERMINAL_GET_SUBSCRIBER_ACTIVE_CODES,
    
    // ===== Agent Operations =====
    REQUEST_REGISTER_CUSTOMER,
    RESPONSE_REGISTER_CUSTOMER,
    
    REQUEST_SUBSCRIBERS_LIST,
    RESPONSE_SUBSCRIBERS_LIST,
    
    REQUEST_WAITING_LIST,
    RESPONSE_WAITING_LIST,
    
    REQUEST_LEAVE_WAITING_LIST,
    RESPONSE_LEAVE_WAITING_LIST,

    REQUEST_AGENT_RESERVATIONS_LIST,
    RESPONSE_AGENT_RESERVATIONS_LIST,
    
    
    REQUEST_WAITING_ADD,    // Agent adds a walk-in customer
    RESPONSE_WAITING_ADD,
    
    REQUEST_WAITING_REMOVE, // Agent cancels a waiting customer
    RESPONSE_WAITING_REMOVE,
    
   
    REQUEST_CURRENT_DINERS,
    RESPONSE_CURRENT_DINERS,
    
    REQUEST_SUBSCRIBER_HISTORY,  
    RESPONSE_SUBSCRIBER_HISTORY, 
    
 // ===== Table Management (Agent) =====
    REQUEST_TABLES_GET,
    RESPONSE_TABLES_GET,

    REQUEST_TABLE_ADD,
    RESPONSE_TABLE_ADD,

    REQUEST_TABLE_REMOVE,
    RESPONSE_TABLE_REMOVE,
    
    REQUEST_TABLE_UPDATE,
    RESPONSE_TABLE_UPDATE,
    
 // ===== Opening Hours (Agent) =====
    REQUEST_OPENING_HOURS_GET,
    RESPONSE_OPENING_HOURS_GET,
    
    REQUEST_OPENING_HOURS_UPDATE,      // Update existing row (Regular or Special)
    RESPONSE_OPENING_HOURS_UPDATE,
    
    REQUEST_OPENING_HOURS_ADD_SPECIAL, // Add new specific date
    RESPONSE_OPENING_HOURS_ADD_SPECIAL,
    
    REQUEST_OPENING_HOURS_REMOVE,      // Remove a special date
    RESPONSE_OPENING_HOURS_REMOVE,
    
    REQUEST_TODAY_HOURS,
    RESPONSE_TODAY_HOURS,
    
 // ===== Reports (Manager Only) =====
    REQUEST_REPORT_PERFORMANCE,
    RESPONSE_REPORT_PERFORMANCE,
    
    REQUEST_REPORT_ACTIVITY,
    RESPONSE_REPORT_ACTIVITY,
    
    REQUEST_PERFORMANCE_LOGS,  
    RESPONSE_PERFORMANCE_LOGS,
}