package net.technicpack.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project

internal const val WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY = "windowsInstallerJreMajorVersion"
internal const val DEFAULT_WINDOWS_INSTALLER_JRE_MAJOR_VERSION = 25
internal const val WINDOWS_INSTALLER_STABLE_LAUNCHER_DOWNLOAD_URL = "https://launcher.technicpack.net/launcher4/stable/TechnicLauncher.exe"
internal const val WINDOWS_INSTALLER_LAUNCHER_EXECUTABLE_NAME = "launcher.exe"
internal const val WINDOWS_INSTALLER_RUNTIME_DIRECTORY = "launcher-runtime"

data class WindowsInstallerMetadata(
    val launcherDownloadUrl: String = WINDOWS_INSTALLER_STABLE_LAUNCHER_DOWNLOAD_URL,
    val jreMajorVersion: Int = DEFAULT_WINDOWS_INSTALLER_JRE_MAJOR_VERSION,
    val installRoot: String = "\$APPDATA\\.technic",
    val launcherExecutableName: String = WINDOWS_INSTALLER_LAUNCHER_EXECUTABLE_NAME,
    val runtimeDirectory: String = WINDOWS_INSTALLER_RUNTIME_DIRECTORY,
    val bundledJrePath: String = WINDOWS_INSTALLER_RUNTIME_DIRECTORY,
    val runtimeExecutablePath: String = "\$INSTDIR\\$WINDOWS_INSTALLER_RUNTIME_DIRECTORY\\bin\\javaw.exe",
    val desktopShortcutDefault: Boolean = true,
    val launchAfterInstallDefault: Boolean = true,
    val uninstallRegistryRoot: String = "HKCU",
) {
    companion object {
        fun from(
            launcherDownloadUrl: String?,
            jreMajorVersion: String?,
        ): WindowsInstallerMetadata =
            WindowsInstallerMetadata(
                launcherDownloadUrl = launcherDownloadUrl ?: WINDOWS_INSTALLER_STABLE_LAUNCHER_DOWNLOAD_URL,
                jreMajorVersion =
                    jreMajorVersion?.toIntOrNull()
                        ?: if (jreMajorVersion == null) {
                            DEFAULT_WINDOWS_INSTALLER_JRE_MAJOR_VERSION
                        } else {
                            throw GradleException(
                                "Property '$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY' must be a whole number, but was '$jreMajorVersion'.",
                            )
                        },
            ).also { metadata ->
                if (metadata.jreMajorVersion <= 0) {
                    throw GradleException(
                        "Property '$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY' must be greater than zero, but was '${metadata.jreMajorVersion}'.",
                    )
                }
            }
    }
}

internal fun Project.windowsInstallerMetadata(): WindowsInstallerMetadata =
    WindowsInstallerMetadata.from(
        WINDOWS_INSTALLER_STABLE_LAUNCHER_DOWNLOAD_URL,
        providers.gradleProperty(WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY).orNull,
    )
