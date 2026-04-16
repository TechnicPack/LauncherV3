/*
 * This file is part of Technic Minecraft Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic Minecraft Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Minecraft Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Minecraft Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.minecraftcore.mojang.version.chain;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.technicpack.launchercore.launch.java.IJavaRuntime;
import net.technicpack.minecraftcore.launch.ILaunchOptions;
import net.technicpack.minecraftcore.mojang.version.IMinecraftVersionInfo;
import net.technicpack.minecraftcore.mojang.version.io.*;
import net.technicpack.minecraftcore.mojang.version.io.argument.Argument;
import net.technicpack.minecraftcore.mojang.version.io.argument.ArgumentList;
import net.technicpack.utilslib.Utils;

public class ChainedMinecraftVersionInfo implements IMinecraftVersionInfo {

  private List<IMinecraftVersionInfo> chain;
  private VersionJavaInfo overrideJavaInfo;

  public ChainedMinecraftVersionInfo(IMinecraftVersionInfo rootVersion) {
    chain = new LinkedList<>();
    chain.add(rootVersion);
  }

  @Override
  public String getId() {
    return chain.get(0).getId();
  }

  @Override
  public ReleaseType getType() {
    return chain.get(0).getType();
  }

  @Override
  public ArgumentList getMinecraftArguments() {
    ArgumentList.Builder allArguments = new ArgumentList.Builder();

    for (IMinecraftVersionInfo version : chain) {
      if (version.getMinecraftArguments() != null) {
        for (Argument arg : version.getMinecraftArguments().getArguments()) {
          allArguments.addArgument(arg);
        }
      }
    }

    return allArguments.build();
  }

  @Override
  public ArgumentList getJavaArguments() {
    ArgumentList.Builder allArguments = new ArgumentList.Builder();

    for (IMinecraftVersionInfo version : chain) {
      if (version.getJavaArguments() != null) {
        for (Argument arg : version.getJavaArguments().getArguments()) {
          allArguments.addArgument(arg);
        }
      }
    }

    return allArguments.build();
  }

  @Override
  public ArgumentList getDefaultUserJavaArguments() {
    ArgumentList.Builder allArguments = new ArgumentList.Builder();
    boolean hasDefaultUserJavaArguments = false;

    for (IMinecraftVersionInfo version : chain) {
      if (version.getDefaultUserJavaArguments() != null) {
        hasDefaultUserJavaArguments = true;
        for (Argument arg : version.getDefaultUserJavaArguments().getArguments()) {
          allArguments.addArgument(arg);
        }
      }
    }

    if (!hasDefaultUserJavaArguments) {
      return null;
    }

    return allArguments.build();
  }

  @Override
  public List<Library> getLibraries() {
    List<Library> allLibraries = new LinkedList<>();

    for (IMinecraftVersionInfo version : chain) {
      if (version.getLibraries() != null) allLibraries.addAll(0, version.getLibraries());
    }

    return allLibraries.stream().distinct().collect(Collectors.toList());
  }

  @Override
  public List<Library> getLibrariesForCurrentOS(ILaunchOptions options, IJavaRuntime runtime) {
    List<Library> allLibraries = new LinkedList<>();

    for (int i = chain.size() - 1; i >= 0; i--) {
      IMinecraftVersionInfo version = chain.get(i);
      List<Library> librariesForCurrentOS = version.getLibrariesForCurrentOS(options, runtime);
      if (librariesForCurrentOS != null) allLibraries.addAll(0, librariesForCurrentOS);
    }

    return allLibraries.stream().distinct().collect(Collectors.toList());
  }

  @Override
  public String getMainClass() {
    for (IMinecraftVersionInfo version : chain) {
      if (version.getMainClass() != null) return version.getMainClass();
    }

    return null;
  }

  @Override
  public void setMainClass(String mainClass) {
    chain.get(0).setMainClass(mainClass);
  }

  @Override
  public List<Rule> getRules() {
    List<Rule> allRules = new LinkedList<>();

    for (IMinecraftVersionInfo version : chain) {
      if (version.getRules() != null) allRules.addAll(0, version.getRules());
    }

    return allRules;
  }

  @Override
  public AssetIndex getAssetIndex() {
    for (IMinecraftVersionInfo version : chain) {
      if (version.getAssetIndex() != null) return version.getAssetIndex();
    }

    return null;
  }

  @Override
  public String getAssetsKey() {
    for (IMinecraftVersionInfo version : chain) {
      if (version.getAssetsKey() != null) return version.getAssetsKey();
    }

    return null;
  }

  @Override
  public GameDownloads getDownloads() {
    for (IMinecraftVersionInfo version : chain) {
      if (version.getDownloads() != null) return version.getDownloads();
    }

    return null;
  }

  @Override
  public boolean getAreAssetsVirtual() {
    return chain.get(0).getAreAssetsVirtual();
  }

  @Override
  public void setAreAssetsVirtual(boolean areAssetsVirtual) {
    chain.get(0).setAreAssetsVirtual(areAssetsVirtual);
  }

  @Override
  public boolean getAssetsMapToResources() {
    return chain.get(0).getAssetsMapToResources();
  }

  @Override
  public void setAssetsMapToResources(boolean mapToResources) {
    chain.get(0).setAssetsMapToResources(mapToResources);
  }

  @Override
  public String getParentVersion() {
    return chain.get(chain.size() - 1).getId();
  }

  @Override
  public void addLibrary(Library library) {
    chain.get(0).addLibrary(library);
  }

  @Override
  public void prependLibrary(Library library) {
    chain.get(0).prependLibrary(library);
  }

  @Override
  public VersionJavaInfo getMojangRuntimeInformation() {
    if (overrideJavaInfo != null) return overrideJavaInfo;

    for (IMinecraftVersionInfo version : chain) {
      if (version.getMojangRuntimeInformation() != null)
        return version.getMojangRuntimeInformation();
    }

    return null;
  }

  @Override
  public void removeLibrary(String libraryName) {
    for (IMinecraftVersionInfo version : chain) {
      version.removeLibrary(libraryName);
    }
  }

  @Override
  public void setMojangRuntimeInformation(VersionJavaInfo info) {
    this.overrideJavaInfo = info;
  }

  @Override
  public void addJvmArguments(List<Argument> args) {
    chain.get(0).addJvmArguments(args);
  }

  @Override
  public void addGameArguments(List<Argument> args) {
    chain.get(0).addGameArguments(args);
  }

  @Override
  public void replaceAllLibraries(List<Library> replacementLibraries) {
    // Snapshot what we're about to remove, per chain layer, so the operation is auditable in logs.
    // Required because a Prism patch that supplies an incomplete library list will silently wipe
    // libraries contributed by intermediate chain layers (e.g. a Forge inheritance layer).
    StringBuilder summary = new StringBuilder();
    summary
        .append("ChainedMinecraftVersionInfo.replaceAllLibraries: clearing ")
        .append(chain.size())
        .append(" chain layer(s) and adding ")
        .append(replacementLibraries.size())
        .append(" replacement librar")
        .append(replacementLibraries.size() == 1 ? "y" : "ies")
        .append('.');

    for (IMinecraftVersionInfo version : chain) {
      List<String> removedNames = new ArrayList<>();
      for (Library lib : version.getLibraries()) {
        removedNames.add(lib.getName());
      }
      if (!removedNames.isEmpty()) {
        summary
            .append("\n  cleared from layer '")
            .append(version.getId())
            .append("' (")
            .append(removedNames.size())
            .append("): ")
            .append(String.join(", ", removedNames));
      }
      for (String name : removedNames) {
        version.removeLibrary(name);
      }
    }

    if (!replacementLibraries.isEmpty()) {
      List<String> addedNames =
          replacementLibraries.stream().map(Library::getName).collect(Collectors.toList());
      summary
          .append("\n  added to layer '")
          .append(chain.get(0).getId())
          .append("': ")
          .append(String.join(", ", addedNames));
    }

    Utils.getLogger().log(Level.INFO, summary.toString());

    // Add replacement libraries to the root version
    for (Library lib : replacementLibraries) {
      chain.get(0).addLibrary(lib);
    }
  }

  @Override
  public IJavaRuntime getJavaRuntime() {
    for (IMinecraftVersionInfo version : chain) {
      if (version.getJavaRuntime() != null) return version.getJavaRuntime();
    }

    return null;
  }

  @Override
  public void setJavaRuntime(IJavaRuntime runtime) {
    for (IMinecraftVersionInfo version : chain) {
      version.setJavaRuntime(runtime);
    }
  }

  public void addVersionToChain(IMinecraftVersionInfo version) {
    chain.add(version);
  }
}
