package com.pcr.dataserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import static java.nio.file.StandardOpenOption.*;
import org.springframework.stereotype.Component;

/** Content addressed files: client FileName is metadata, never a filesystem path. */
@Component
public class FileStore {
    private final Path root;
    public FileStore(ServerSettings settings) throws IOException {
        root = settings.storage().toAbsolutePath().normalize(); Files.createDirectories(root);
    }
    public Path path(String hash) {
        if (hash == null || !hash.matches("[a-f0-9]{64}")) throw ApiException.bad("Invalid file hash");
        return root.resolve(hash.substring(0,2)).resolve(hash + ".bin");
    }
    public String put(byte[] bytes) throws IOException {
        String hash = Digests.hash("SHA-256", bytes);
        Path target = path(hash); Files.createDirectories(target.getParent());
        if (Files.isRegularFile(target)) {
            if (!Digests.hash("SHA-256", Files.readAllBytes(target)).equals(hash))
                throw new IOException("Stored file integrity check failed");
            return hash;
        }
        Path temp = Files.createTempFile(target.getParent(), ".incoming-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp, WRITE, TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            catch (FileAlreadyExistsException e) { /* concurrent identical content writer */ }
        } finally { Files.deleteIfExists(temp); }
        return hash;
    }
}
