package net.technicpack.launchercore.install.tasks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.technicpack.launchercore.exception.DownloadException;
import net.technicpack.launchercore.install.InstalledPack;
import net.technicpack.launchercore.minecraft.MojangConstants;
import net.technicpack.launchercore.util.Utils;
import net.technicpack.launchercore.util.verifiers.FileSizeVerifier;
import net.technicpack.launchercore.util.verifiers.IFileVerifier;
import net.technicpack.launchercore.util.verifiers.ValidJsonFileVerifier;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

public class GetAssetsIndexTask extends ListenerTask {
    private InstalledPack pack;

    public GetAssetsIndexTask(InstalledPack pack) {
        this.pack = pack;
    }

    @Override
    public String getTaskDescription() {
        return "Retrieving assets index";
    }

    @Override
    public void runTask(InstallTasksQueue queue) throws IOException {
        super.runTask(queue);

        String assets = queue.getCompleteVersion().getAssetsKey();

        if (assets == null || assets.isEmpty()) {
            assets = "legacy";
        }

        File output = new File(Utils.getAssetsDirectory() + File.separator + "indexes", assets + ".json");

        (new File(output.getParent())).mkdirs();

        IFileVerifier fileVerifier = new ValidJsonFileVerifier();
        String assetsUrl = MojangConstants.getAssetsIndex(assets);

        if (!output.exists() || !fileVerifier.isFileValid(output)) {
            queue.getMirrorStore().downloadFile(assetsUrl, output.getName(), output.getAbsolutePath(), null, fileVerifier, this);
        }

        String json = FileUtils.readFileToString(output, Charset.forName("UTF-8"));
        JsonObject obj = Utils.getMojangGson().fromJson(json, JsonObject.class);

        if (obj == null) {
            throw new DownloadException("The assets json file was invalid.");
        }

        boolean isVirtual = false;

        if (obj.get("virtual") != null)
            isVirtual = obj.get("virtual").getAsBoolean();

        queue.getCompleteVersion().setAreAssetsVirtual(isVirtual);

        JsonObject allObjects = obj.get("objects").getAsJsonObject();

        if (allObjects == null) {
            throw new DownloadException("The assets json file was invalid.");
        }

        for (Map.Entry<String, JsonElement> field : allObjects.entrySet()) {
            String friendlyName = field.getKey();
            JsonObject file = field.getValue().getAsJsonObject();
            String hash = file.get("hash").getAsString();
            long size = file.get("size").getAsLong();

            File location = new File(Utils.getAssetsDirectory() + File.separator + "objects" + File.separator + hash.substring(0, 2) + File.separator, hash);
            String url = MojangConstants.getResourceUrl(hash);

            (new File(location.getParent())).mkdirs();

            File virtualOut = new File(Utils.getAssetsDirectory() + File.separator + "virtual" + File.separator + assets + File.separator + friendlyName);

            queue.AddTask(new EnsureFileTask(location, new FileSizeVerifier(size), null, url, virtualOut.getName()));

            if (isVirtual && !virtualOut.exists()) {
                (new File(virtualOut.getParent())).mkdirs();
                queue.AddTask(new CopyFileTask(location, virtualOut));
            }
        }
    }
}
