package common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;

public final class KryoUtil {

    // One Kryo per thread (Kryo is NOT thread-safe)
    private static final ThreadLocal<Kryo> KRYO = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();

        // ✅ Production/submission mode: require registration (safer + faster + deterministic)
        kryo.setRegistrationRequired(true);

        // IMPORTANT: client + server must register the SAME classes in the SAME order.
        registerAll(kryo);

        return kryo;
    });

    private KryoUtil() {}

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

    public static byte[] toBytes(Object obj) {
        Kryo kryo = KRYO.get();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Output output = new Output(baos)) {
            kryo.writeClassAndObject(output, obj);
        }
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromBytes(byte[] bytes) {
        Kryo kryo = KRYO.get();
        try (Input input = new Input(new ByteArrayInputStream(bytes))) {
            return (T) kryo.readClassAndObject(input);
        }
    }
}

