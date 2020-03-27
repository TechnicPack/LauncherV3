/*
 * This file is part of Technic Launcher Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launchercore.launch.java.source.os;


import net.technicpack.launchercore.launch.java.IVersionSource;
import net.technicpack.launchercore.launch.java.JavaVersionRepository;
import net.technicpack.launchercore.launch.java.version.FileBasedJavaVersion;
import net.technicpack.utilslib.Utils;

import java.io.File;

/**
 * Pull the installed java versions from the registry, using REG QUERY command line utility
 *
 */
public class WinRegistryJavaSource implements IVersionSource {
    @Override
    public void enumerateVersions(JavaVersionRepository repository) {
        enumerateRegistryForBitness(repository, "32");
        enumerateRegistryForBitness(repository, "64");
    }

    public void enumerateRegistryForBitness(JavaVersionRepository repository, String bitness) {
        String output = Utils.getProcessOutput("reg","query","HKEY_LOCAL_MACHINE\\Software\\JavaSoft\\","/f","Home","/t","REG_SZ","/s","/reg:"+bitness);

        if (output == null || output.isEmpty())
            return;

        //Split reg query response into lines
        for (String line : output.split("\\r?\\n")) {

            //Reg query is a response that's like
            //Key Path
            //Key Name          Type            Value

            //We want to grab all the values that were returned and parse them into java home paths.
            //The type is always REG_SZ because that's what we searched for, so we find the type & expect the value to be afterward

            int typeIndex = line.indexOf("REG_SZ");

            if (typeIndex < 0)
                continue;

            typeIndex += "REG_SZ".length();

            String path = line.substring(typeIndex).trim();
            repository.addVersion(new FileBasedJavaVersion(new File(path + File.separator + "bin" + File.separator + "javaw.exe")));
        }
    }
}
