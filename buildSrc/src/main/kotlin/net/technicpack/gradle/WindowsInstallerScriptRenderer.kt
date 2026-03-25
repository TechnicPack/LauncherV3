package net.technicpack.gradle

import org.gradle.api.Project

internal const val WINDOWS_INSTALLER_SCRIPT_TEMPLATE_PATH = "src/installer/windows/TechnicLauncherBootstrap.iss"

internal fun Project.renderWindowsInstallerScript(metadata: WindowsInstallerMetadata = windowsInstallerMetadata()): String =
    renderWindowsInstallerScript(
        template =
            layout.projectDirectory
                .file(WINDOWS_INSTALLER_SCRIPT_TEMPLATE_PATH)
                .asFile
                .readText(),
        metadata = metadata,
    )

internal fun renderWindowsInstallerScript(
    template: String,
    metadata: WindowsInstallerMetadata,
): String {
    val launcherDownloadUrl = buildStableLauncherDownloadUrl(metadata.launcherDownloadUrl)
    val runtimeArchiveUrl = buildAdoptiumWindowsX64JreArchiveUrl(metadata.jreMajorVersion)
    val replacements =
        mapOf(
            "launcherDownloadUrl" to launcherDownloadUrl.innoEscape(),
            "runtimeArchiveUrl" to runtimeArchiveUrl.innoEscape(),
            "defaultDirName" to metadata.installRoot.toInnoConstantPath().innoEscape(),
            "launcherExecutableName" to metadata.launcherExecutableName.innoEscape(),
            "runtimeDirectory" to metadata.runtimeDirectory.innoEscape(),
            "runtimeArchiveExecutableRelativePath" to "bin\\javaw.exe".innoEscape(),
            "desktopShortcutFlags" to metadata.desktopShortcutDefault.toInnoTaskFlags(),
            "launchAfterInstallFlags" to metadata.launchAfterInstallDefault.toInnoRunFlags(),
            "uninstallRegistryRoot" to metadata.uninstallRegistryRoot.innoEscape(),
        )

    return replacements.entries.fold(template) { rendered, (key, value) ->
        rendered.replace("{{ $key }}", value)
    }
}

private fun String.innoEscape(): String =
    buildString(length) {
        for (character in this@innoEscape) {
            when (character) {
                '"' -> append("\"\"")
                '\'' -> append("''")
                '\r', '\n' -> append(' ')
                else -> append(character)
            }
        }
    }

private fun Boolean.toInnoTaskFlags(): String = if (this) "checkedonce" else ""

private fun Boolean.toInnoRunFlags(): String =
    buildString {
        append("nowait postinstall skipifsilent")
        if (!this@toInnoRunFlags) {
            append(" unchecked")
        }
    }

private fun String.toInnoConstantPath(): String =
    this
        .replace("\$APPDATA", "{userappdata}")
        .replace("\$INSTDIR", "{app}")
