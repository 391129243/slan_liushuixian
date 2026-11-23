import com.google.gson.*;
import com.hongshi.slansdk.model.experiment.Experiment;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Type;

/** Reproduces the reference SLANManager serializer on real vendor-derived values. */
public class ProbeAndroidGson {
    public static void main(String[] args) throws Exception {
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
            @Override public JsonElement serialize(Double value, Type type, JsonSerializationContext context) {
                if (value == value.longValue()) return new JsonPrimitive(value.longValue());
                return new JsonPrimitive(value);
            }
        }).create();
        String input = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        Experiment experiment = gson.fromJson(input, Experiment.class);
        try {
            String serialized = gson.toJson(experiment);
            Files.writeString(Path.of(args[1]).resolveSibling("android-original-upload.json.txt"), serialized, StandardCharsets.UTF_8);
            if (!serialized.contains(":NaN")) throw new IllegalStateException("Expected non-standard NaN tokens in the reference serializer output");
            String output = "CONFIRMED: reference SLANManager serializer with Gson 2.8.9 emits bare NaN tokens, which are NOT standard JSON. This is a local serializer reproduction, not a device run.";
            Files.writeString(Path.of(args[1]), output, StandardCharsets.UTF_8);
            System.out.println(output);
        } catch (IllegalArgumentException expected) {
            String output = "CONFIRMED: unchanged SLANManager Gson serializer rejects non-finite results before HTTP upload: " + expected.getMessage();
            Files.writeString(Path.of(args[1]), output, StandardCharsets.UTF_8);
            System.out.println(output);
        }
    }
}
