import com.google.gson.JsonObject;

public class SafeExperimentJsonCheck {
    static class Target {
        double Ct = Double.NaN;
        Double CnValue = Double.POSITIVE_INFINITY;
        float Signal = Float.NEGATIVE_INFINITY;
        double RealZero = 0;
        double Tm = 48.46;
        String SampleName = "NaN";
    }
    public static void main(String[] args) {
        Target target = new Target();
        String json = SafeExperimentJson.serialize(target);
        JsonObject object = SafeExperimentJson.create().fromJson(json, JsonObject.class);
        if (!object.get("Ct").isJsonNull() || !object.get("CnValue").isJsonNull() || !object.get("Signal").isJsonNull()
                || object.get("RealZero").getAsDouble() != 0 || object.get("Tm").getAsDouble() != 48.46
                || !object.get("SampleName").getAsString().equals("NaN") || !Double.isNaN(target.Ct)) {
            throw new AssertionError("Non-finite numeric normalization corrupted values or source object");
        }
        System.out.println("PASS: NaN/Infinity become explicit null; valid zero, Tm, text and source object remain unchanged.");
    }
}
