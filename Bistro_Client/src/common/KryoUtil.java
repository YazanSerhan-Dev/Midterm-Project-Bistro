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

        // ✅ During development: easiest + prevents "Class is not registered" errors.
        // Later (before submission), we can turn this back to true and register everything in same order on server+client.
        kryo.setRegistrationRequired(false);

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
