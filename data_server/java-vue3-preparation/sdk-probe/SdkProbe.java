import java.nio.file.*;
import java.lang.reflect.*;

/** Inspects public APIs without initializing the vendor native library. */
public class SdkProbe {
    public static void main(String[] args) throws Exception {
        Path library = Path.of(args.length == 0 ? "../vendor/libslansdk.so" : args[0]);
        byte[] header = Files.readAllBytes(library);
        if (header.length < 64 || header[0] != 0x7f || header[1] != 'E'
                || header[2] != 'L' || header[3] != 'F') {
            throw new IllegalArgumentException("Not an ELF library");
        }
        int machine = (header[18] & 255) | ((header[19] & 255) << 8);
        System.out.printf("Host: %s %s%nELF: class=%d machine=%d (183=AArch64)%n",
            System.getProperty("os.name"), System.getProperty("os.arch"), header[4], machine);
        for (String name : new String[]{"com.hongshi.slansdk.service.SLANService",
                "com.hongshi.slansdk.model.experiment.TargetParamter",
                "com.hongshi.slansdk.model.result.TargetResult"}) {
            Class<?> type = Class.forName(name, false, SdkProbe.class.getClassLoader());
            for (Method method : type.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic())
                    System.out.println(method);
            }
        }
        System.out.println("Static inspection passed. Native loading and PCR analysis NOT performed.");
    }
}
