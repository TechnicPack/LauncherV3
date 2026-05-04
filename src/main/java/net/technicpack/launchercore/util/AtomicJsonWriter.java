package net.technicpack.launchercore.util;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import net.technicpack.utilslib.Utils;

/**
 * Writes JSON to a target path with durable, crash-safe semantics:
 *
 * <ol>
 *   <li>Serialize to a sibling {@code .tmp} file.
 *   <li>{@code fsync} the {@code .tmp} (data + metadata).
 *   <li>Atomically rename it over the target.
 * </ol>
 *
 * <p>A JVM crash or OS crash during step 1 or 2 leaves the original target untouched; the orphaned
 * {@code .tmp} is overwritten on the next save. A crash after step 3 has completed has already
 * persisted the new contents. This closes the usual "settings file ends up empty after a power cut"
 * failure mode that plain {@link Files#newBufferedWriter} leaves open.
 *
 * <p>Falls back to a non-atomic replace on filesystems that don't support {@code ATOMIC_MOVE}
 * (FAT32, some network mounts). On {@link IOException} or {@link JsonIOException} during the write,
 * the {@code .tmp} is removed before the exception propagates so callers aren't left cleaning up
 * partial state.
 *
 * <p>Parent-directory creation is the caller's responsibility.
 */
public final class AtomicJsonWriter {

  private AtomicJsonWriter() {}

  public static void write(Path target, Object payload, Gson gson) throws IOException {
    Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
    try {
      try (FileChannel channel =
              FileChannel.open(
                  tmp,
                  StandardOpenOption.CREATE,
                  StandardOpenOption.TRUNCATE_EXISTING,
                  StandardOpenOption.WRITE);
          Writer writer =
              new BufferedWriter(
                  new OutputStreamWriter(
                      Channels.newOutputStream(channel), StandardCharsets.UTF_8))) {
        gson.toJson(payload, writer);
        writer.flush();
        channel.force(true);
      }
      try {
        Files.move(
            tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Utils.getLogger()
            .warning("Filesystem does not support atomic move; falling back to non-atomic replace");
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException | JsonIOException e) {
      try {
        Files.deleteIfExists(tmp);
      } catch (IOException ignored) {
        // Best-effort cleanup; don't mask the original failure.
      }
      throw e;
    }
  }
}
