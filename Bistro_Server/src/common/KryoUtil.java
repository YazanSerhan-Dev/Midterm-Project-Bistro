package common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
/**
 * Utility class responsible for Kryo serialization and deserialization.
 *
 * <p>
 * {@code KryoUtil} provides a centralized and consistent way to convert
 * objects into byte arrays and back, using the Kryo serialization framework.
 * It is used by both client and server to ensure compatible binary
 * communication.
 * </p>
 *
 * <p>
 * This class uses a {@link ThreadLocal} Kryo instance because Kryo
 * is <b>not thread-safe</b>. Each thread gets its own Kryo instance
 * with an identical registration configuration.
 * </p>
 *
 * <p>
 * Registration is mandatory ({@code setRegistrationRequired(true)}),
 * meaning that both client and server must register the exact same
 * classes in the exact same order to guarantee deterministic
 * serialization.
 * </p>
 */
public final class KryoUtil {

	   /**
     * Thread-local Kryo instance.
     *
     * <p>
     * Each thread lazily initializes its own Kryo object to avoid
     * concurrency issues while maintaining high performance.
     * </p>
     */
    private static final ThreadLocal<Kryo> KRYO = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();

     // Require explicit registration for safety and performance
        kryo.setRegistrationRequired(true);

        // IMPORTANT: client + server must register the SAME classes in the SAME order.
        registerAll(kryo);

        return kryo;
    });
    /**
     * Private constructor to prevent instantiation.
     */
    private KryoUtil() {}
    /**
     * Registers all classes that may be serialized during
     * client-server communication.
     *
     * <p>
     * The registration order must be identical on both
     * client and server sides.
     * </p>
     *
     * @param kryo the Kryo instance to configure
     */
    private static void registerAll(Kryo kryo) {
        // ---- Transport / protocol ----
        kryo.register(byte[].class);
        kryo.register(Object[].class);
        kryo.register(ArrayList.class);

        kryo.register(common.KryoMessage.class);
        kryo.register(common.Envelope.class);
        kryo.register(common.OpCode.class);

        // ---- Common JDK types used inside DTOs/payloads ----
        kryo.register(Timestamp.class);
        kryo.register(java.time.Instant.class);
        kryo.register(java.time.LocalDate.class);
        kryo.register(java.time.LocalDateTime.class);
        kryo.register(java.time.LocalTime.class);

        // ---- DTOs (common.dto) ----
        kryo.register(common.dto.BillDTO.class);
        kryo.register(common.dto.CurrentDinersDTO.class);
        kryo.register(common.dto.HistoryDTO.class);
        kryo.register(common.dto.LoginRequestDTO.class);
        kryo.register(common.dto.LoginResponseDTO.class);
        kryo.register(common.dto.MakeReservationRequestDTO.class);
        kryo.register(common.dto.MakeReservationResponseDTO.class);
        kryo.register(common.dto.OpeningHoursDTO.class);
        kryo.register(common.dto.ProfileDTO.class);
        kryo.register(common.dto.RegistrationDTO.class);
        kryo.register(common.dto.ReportDTO.class);
        kryo.register(common.dto.ReportRequestDTO.class);
        kryo.register(common.dto.ReservationDTO.class);
        kryo.register(common.dto.RestaurantTableDTO.class);
        kryo.register(common.dto.SubscriberDTO.class);
        kryo.register(common.dto.TerminalActiveItemDTO.class);
        kryo.register(common.dto.TerminalValidateResponseDTO.class);
        kryo.register(common.dto.WaitingListDTO.class);

        // If you still use this anywhere in network payloads, register it too:
        // kryo.register(common.dto.ResolveSubscriberQrResponseDTO.class);
    }
    /**
     * Serializes an object into a byte array using Kryo.
     *
     * @param obj the object to serialize
     * @return Kryo-serialized byte array
     */
    public static byte[] toBytes(Object obj) {
        Kryo kryo = KRYO.get();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Output output = new Output(baos)) {
            kryo.writeClassAndObject(output, obj);
        }
        return baos.toByteArray();
    }
    /**
     * Deserializes a byte array back into an object.
     *
     * <p>
     * The caller is responsible for knowing the expected
     * return type.
     * </p>
     *
     * @param bytes Kryo-serialized byte array
     * @param <T> expected object type
     * @return deserialized object
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromBytes(byte[] bytes) {
        Kryo kryo = KRYO.get();
        try (Input input = new Input(new ByteArrayInputStream(bytes))) {
            return (T) kryo.readClassAndObject(input);
        }
    }
}

