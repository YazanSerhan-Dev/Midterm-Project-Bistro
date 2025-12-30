package common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class KryoUtil {

    // One Kryo per thread (Kryo is NOT thread-safe)
    private static final ThreadLocal<Kryo> KRYO = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();

        // ✅ Register all classes you will send (MUST match on client & server)
        // Example DTOs (you'll add your own):
        // kryo.register(common.dto.GetReservationsRequest.class);
        // kryo.register(common.dto.ReservationDTO.class);
        // kryo.register(java.util.ArrayList.class);

        // If you don't want to register everything manually (not recommended for grading),
        // you can do:
        // kryo.setRegistrationRequired(false);

        kryo.setRegistrationRequired(true); // better for safety + grading
        return kryo;
    });

    private KryoUtil() {}

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
