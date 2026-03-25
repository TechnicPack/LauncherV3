package net.technicpack.gradle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class WindowsInstallerResolversTest {
    private val jreMajorVersion = readWindowsInstallerJreMajorVersion()

    @Test
    fun buildsStableLauncherDownloadUrlFromConfiguredSymlink() {
        assertEquals(
            "https://launcher.technicpack.net/launcher4/stable/TechnicLauncher.exe",
            buildStableLauncherDownloadUrl(WINDOWS_INSTALLER_STABLE_LAUNCHER_DOWNLOAD_URL),
        )
    }

    @Test
    fun buildsAdoptiumWindowsX64JreArchiveUrlForConfiguredMajorVersion() {
        assertEquals(
            "https://api.adoptium.net/v3/binary/latest/$jreMajorVersion/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk",
            buildAdoptiumWindowsX64JreArchiveUrl(jreMajorVersion),
        )
    }

    private fun readWindowsInstallerJreMajorVersion(): Int {
        val gradlePropertiesPath =
            generateSequence(Path.of("").toAbsolutePath().normalize()) { it.parent }
                .map { it.resolve("gradle.properties") }
                .firstOrNull(Files::isRegularFile)
                ?: throw IllegalStateException("Could not locate gradle.properties")

        val properties = Properties()

        Files.newBufferedReader(gradlePropertiesPath).use { reader ->
            properties.load(reader)
        }

        return properties
            .getProperty(WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY)
            ?.toInt()
            ?: throw IllegalStateException(
                "Missing $WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY in ${gradlePropertiesPath.toAbsolutePath()}",
            )
    }
}
