import com.cwjn.skada.data.gen.weapon.WeaponProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

public class WeaponProfileDecodeTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Test
    public void testDecodeAxe() {
        // First, encode the axe profile
        WeaponProfile axe = WeaponProfile.axeTest();

        System.out.println("=== ENCODING ===");
        var encodeResult = WeaponProfile.CODEC.encodeStart(JsonOps.INSTANCE, axe);
        if (encodeResult.error().isPresent()) {
            System.err.println("Encode error: " + encodeResult.error().get());
            return;
        }

        JsonElement encoded = encodeResult.result().get();
        System.out.println("Encoded JSON:");
        System.out.println(GSON.toJson(encoded));

        System.out.println("\n=== DECODING ===");
        var decodeResult = WeaponProfile.CODEC.parse(JsonOps.INSTANCE, encoded);
        if (decodeResult.error().isPresent()) {
            System.err.println("Decode error: " + decodeResult.error().get());
            System.err.println("Full error: " + decodeResult.error().get().message());
        } else {
            System.out.println("Decode successful!");
            WeaponProfile decoded = decodeResult.result().get();
            System.out.println("Handle length: " + decoded.getHandle().getLength());
            System.out.println("Weapon heads: " + decoded.getWeaponHeads().size());
        }
    }
}

