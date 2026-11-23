import com.gidi.bio_pcr.util.ResultBundleUploader;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Runs the exact Android uploader on a desktop JVM; no Android stubs or synthetic network implementation. */
public final class AndroidBundleUploadProbe {
    public static void main(String[] args) throws Exception {
        if(args.length!=5)throw new IllegalArgumentException("experiment-json PCR cache upload-url reply-file");
        String experiment=new String(Files.readAllBytes(Paths.get(args[0])),StandardCharsets.UTF_8);
        CountDownLatch done=new CountDownLatch(1);AtomicBoolean success=new AtomicBoolean();AtomicReference<String> reply=new AtomicReference<>();
        ResultBundleUploader.upload(new JsonParser().parse(experiment),new File(args[1]),new File(args[2]),args[3],(ok,message)->{success.set(ok);reply.set(message);done.countDown();});
        if(!done.await(150,TimeUnit.SECONDS))throw new IllegalStateException("Android uploader timed out");
        Files.write(Paths.get(args[4]),reply.get().getBytes(StandardCharsets.UTF_8));
        if(!success.get())throw new IllegalStateException(reply.get());
        System.out.println("Actual Android ResultBundleUploader completed successfully.");
    }
}
