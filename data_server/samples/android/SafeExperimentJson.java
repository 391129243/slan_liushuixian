import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/** Drop-in serializer example for the reference SLANManager, tested with Gson 2.8.9. */
public final class SafeExperimentJson {
    private SafeExperimentJson() { }
    public static Gson create() {
        JsonSerializer<Double> finiteDouble = new JsonSerializer<Double>() {
            @Override public JsonElement serialize(Double value, Type type, JsonSerializationContext context) {
                if (value == null || value.isNaN() || value.isInfinite()) return JsonNull.INSTANCE;
                if (value == value.longValue()) return new JsonPrimitive(value.longValue());
                return new JsonPrimitive(value);
            }
        };
        JsonSerializer<Float> finiteFloat = new JsonSerializer<Float>() {
            @Override public JsonElement serialize(Float value, Type type, JsonSerializationContext context) {
                return value == null || value.isNaN() || value.isInfinite() ? JsonNull.INSTANCE : new JsonPrimitive(value);
            }
        };
        return new GsonBuilder().serializeNulls()
            .registerTypeAdapter(Double.class, finiteDouble).registerTypeAdapter(double.class, finiteDouble)
            .registerTypeAdapter(Float.class, finiteFloat).registerTypeAdapter(float.class, finiteFloat).create();
    }
    public static String serialize(Object experiment) { return create().toJson(experiment); }
}
