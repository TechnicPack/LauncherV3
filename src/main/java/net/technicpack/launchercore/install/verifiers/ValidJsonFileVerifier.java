package net.technicpack.launchercore.install.verifiers;

import com.google.gson.JsonObject;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.Charset;

public class ValidJsonFileVerifier implements IFileVerifier {
    @Override
    public boolean isFileValid(File file) {
        try {
            String json = FileUtils.readFileToString(file, Charset.forName("UTF-8"));
            JsonObject obj = Utils.getMojangGson().fromJson(json, JsonObject.class);

            return (obj != null);
        } catch (Exception ex) {
            System.out.println("An exception was raised while verifying "+file.getAbsolutePath()+"- this probably just means the file is invalid, in which case this is not an error:");
            ex.printStackTrace();
        }

        return false;
    }
}
