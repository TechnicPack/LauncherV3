package net.technicpack.gradle

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

class WindowsInstallerScriptRendererTest {
    @Test
    fun usesStableWindowsInstallerOutputPathWithoutLauncherVersion() {
        assertEquals(
            "distributions/TechnicLauncherBootstrap.exe",
            buildWindowsInstallerOutputRelativePath(),
        )
    }

    @Test
    fun buildsInnoCompilerCommandLine() {
        assertEquals(
            listOf(
                "docker",
                "run",
                "--rm",
                "-i",
                "-v",
                "/repo:/work",
                "amake/innosetup",
                "/Qp",
                "/Obuild/distributions",
                "/FTechnicLauncherBootstrap",
                "build/installer/windows/TechnicLauncherBootstrap.iss",
            ),
            buildWindowsInstallerCommandLine(
                projectDir = "/repo",
                installerScriptPath = "/repo/build/installer/windows/TechnicLauncherBootstrap.iss",
                installerOutputDir = "/repo/build/distributions",
            ),
        )
    }

    @Test
    fun defaultsMatchBootstrapInstallerContract() {
        val metadata = WindowsInstallerMetadata()

        assertEquals(25, metadata.jreMajorVersion)
        assertEquals("\$APPDATA\\.technic", metadata.installRoot)
        assertEquals("launcher.exe", metadata.launcherExecutableName)
        assertEquals("launcher-runtime", metadata.runtimeDirectory)
        assertEquals("launcher-runtime", metadata.bundledJrePath)
        assertEquals("\$INSTDIR\\launcher-runtime\\bin\\javaw.exe", metadata.runtimeExecutablePath)
        assertFalse(metadata.bundledJrePath.contains("%JAVA_HOME%"))
        assertFalse(metadata.bundledJrePath.contains("%PATH%"))
        assertFalse(metadata.runtimeExecutablePath.contains("%JAVA_HOME%"))
        assertFalse(metadata.runtimeExecutablePath.contains("%PATH%"))
        assertTrue(metadata.desktopShortcutDefault)
        assertTrue(metadata.launchAfterInstallDefault)
        assertEquals("HKCU", metadata.uninstallRegistryRoot)
    }

    @Test
    fun projectPropertyOverridesInstallerJreMajorVersion() {
        val previousValue = System.getProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY")
        System.setProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY", "17")

        try {
            val project = ProjectBuilder.builder().build()

            assertEquals(17, project.windowsInstallerMetadata().jreMajorVersion)
        } finally {
            if (previousValue == null) {
                System.clearProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY")
            } else {
                System.setProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY", previousValue)
            }
        }
    }

    @Test
    fun absentProjectPropertyUsesDefaultInstallerJreMajorVersion() {
        val previousValue = System.getProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY")
        System.clearProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY")

        try {
            val project = ProjectBuilder.builder().build()

            assertEquals(25, project.windowsInstallerMetadata().jreMajorVersion)
        } finally {
            if (previousValue == null) {
                System.clearProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY")
            } else {
                System.setProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY", previousValue)
            }
        }
    }

    @Test
    fun rendersLauncherDownloadAndRuntimeDownloadUrlsFromMetadata() {
        val jreMajorVersion = readWindowsInstallerJreMajorVersion()
        val metadata =
            WindowsInstallerMetadata(
                launcherDownloadUrl = WINDOWS_INSTALLER_STABLE_LAUNCHER_DOWNLOAD_URL,
                jreMajorVersion = jreMajorVersion,
            )

        val rendered =
            renderWindowsInstallerScript(
                template =
                    """
                    Download launcher: {{ launcherDownloadUrl }}
                    Download runtime: {{ runtimeArchiveUrl }}
                    """.trimIndent(),
                metadata = metadata,
            )

        assertTrue(rendered.contains("Download launcher: https://launcher.technicpack.net/launcher4/stable/TechnicLauncher.exe"))
        assertTrue(
            rendered.contains(
                "Download runtime: https://api.adoptium.net/v3/binary/latest/$jreMajorVersion/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk",
            ),
        )
        assertFalse(rendered.contains("{{ launcherDownloadUrl }}"))
        assertFalse(rendered.contains("{{ runtimeArchiveUrl }}"))
    }

    @Test
    fun rendersBootstrapInstallerScriptWithPerUserRepairAndShortcutBehavior() {
        val jreMajorVersion = readWindowsInstallerJreMajorVersion()
        val metadata =
            WindowsInstallerMetadata(
                launcherDownloadUrl = WINDOWS_INSTALLER_STABLE_LAUNCHER_DOWNLOAD_URL,
                jreMajorVersion = jreMajorVersion,
                installRoot = "\$APPDATA\\.technic",
                launcherExecutableName = "launcher.exe",
                runtimeDirectory = "launcher-runtime",
                bundledJrePath = "launcher-runtime",
                runtimeExecutablePath = "\$INSTDIR\\launcher-runtime\\bin\\javaw.exe",
                desktopShortcutDefault = true,
                launchAfterInstallDefault = true,
                uninstallRegistryRoot = "HKCU",
            )

        val templatePath =
            generateSequence(Path.of("").toAbsolutePath().normalize()) { it.parent }
                .map { it.resolve("src/installer/windows/TechnicLauncherBootstrap.iss") }
                .firstOrNull(Files::isRegularFile)
                ?: throw IllegalStateException("Could not locate src/installer/windows/TechnicLauncherBootstrap.iss")

        val rendered = renderWindowsInstallerScript(Files.readString(templatePath), metadata)

        assertAll(
            { assertTrue(rendered.contains("PrivilegesRequired=lowest")) },
            { assertTrue(rendered.contains("DefaultDirName={userappdata}\\.technic")) },
            { assertTrue(rendered.contains("AppVersion=4.0")) },
            { assertTrue(rendered.contains("AppVerName=Technic Launcher 4.0")) },
            { assertTrue(rendered.contains("OutputBaseFilename=TechnicLauncherBootstrap")) },
            { assertTrue(rendered.contains("Name: \"desktopicon\"")) },
            { assertTrue(rendered.contains("Flags: checkedonce")) },
            { assertTrue(rendered.contains("CreateDownloadPage")) },
            { assertTrue(rendered.contains("DownloadPage.Clear;")) },
            { assertTrue(rendered.contains("DownloadPage.ShowBaseNameInsteadOfUrl := True;")) },
            { assertTrue(rendered.contains("DownloadPage.Download;")) },
            { assertTrue(rendered.contains("ExtractArchive")) },
            {
                assertTrue(
                    rendered.contains(
                        "DownloadPage.Add('https://api.adoptium.net/v3/binary/latest/$jreMajorVersion/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk', 'launcher-runtime.zip', '');",
                    ),
                )
            },
            {
                assertTrue(
                    rendered.contains(
                        "DownloadPage.Add('https://launcher.technicpack.net/launcher4/stable/TechnicLauncher.exe', 'launcher.exe', '');",
                    ),
                )
            },
            {
                assertTrue(
                    rendered.contains(
                        "https://api.adoptium.net/v3/binary/latest/$jreMajorVersion/ga/windows/x64/jre/hotspot/normal/eclipse?project=jdk",
                    ),
                )
            },
            { assertTrue(rendered.contains("{app}\\launcher-runtime")) },
            { assertTrue(rendered.contains("FileExists(AddBackslash(ExtractedPath) + 'bin\\javaw.exe')")) },
            { assertTrue(rendered.contains("Root: HKCU")) },
            { assertTrue(rendered.contains("Type: files; Name: \"{app}\\launcher.exe\"")) },
            { assertTrue(rendered.contains("Type: filesandordirs; Name: \"{app}\\launcher-runtime\"")) },
            {
                assertTrue(
                    rendered.contains(
                        "Filename: \"{app}\\launcher.exe\"; Description: \"{cm:LaunchProgram,Technic Launcher}\"; Flags: nowait postinstall skipifsilent",
                    ),
                )
            },
            { assertTrue(rendered.contains("{tmp}\\launcher-runtime-staging")) },
            { assertTrue(rendered.contains("{app}\\launcher-runtime-staging")) },
            { assertTrue(rendered.contains("{app}\\launcher-runtime-previous")) },
            { assertTrue(rendered.contains("{app}\\launcher.exe.previous")) },
            { assertTrue(rendered.contains("{app}\\launcher.exe.staging")) },
            {
                assertTrue(
                    rendered.contains(
                        "RenameFile(AppRuntimePath(), PreviousRuntimePath())",
                    ),
                )
            },
            {
                assertTrue(
                    rendered.contains(
                        "RenameFile(AppRuntimeStagingPath(), AppRuntimePath())",
                    ),
                )
            },
            {
                assertTrue(
                    rendered.contains(
                        "RenameFile(LauncherInstalledPath(), PreviousLauncherPath())",
                    ),
                )
            },
            {
                assertTrue(
                    rendered.contains(
                        "RenameFile(LauncherStagingPath(), LauncherInstalledPath())",
                    ),
                )
            },
            {
                assertTrue(
                    rendered.contains(
                        "RenameFile(PreviousLauncherPath(), LauncherInstalledPath())",
                    ),
                )
            },
            { assertTrue(rendered.contains("Downloaded launcher runtime has unexpected layout")) },
            { assertTrue(rendered.contains("Downloaded launcher runtime is missing bin\\javaw.exe")) },
            { assertTrue(rendered.contains("Downloaded launcher runtime is missing lib")) },
            { assertTrue(rendered.contains("Downloaded launcher runtime is missing release")) },
            { assertTrue(rendered.contains("Delete all launcher data")) },
            { assertTrue(rendered.contains("keep installedPacks and modpacks")) },
            { assertFalse(rendered.contains("PowerShell")) },
            { assertFalse(rendered.contains("nsExec")) },
            { assertFalse(rendered.contains("TechnicLauncherBootstrap.nsi")) },
            { assertFalse(rendered.contains("https://downloads.technicpack.net/version/stable")) },
            { assertFalse(rendered.contains("AddBackslash(ExtractedPath) + 'launcher-runtime\\bin\\javaw.exe'")) },
            {
                assertFalse(
                    rendered.contains(
                        "RenameFile(ExpandConstant('{tmp}\\launcher-runtime-staging'), ExpandConstant('{app}\\launcher-runtime'))",
                    ),
                )
            },
            { assertFalse(rendered.contains("Type: filesandordirs; Name: \"{app}\"")) },
        )
    }

    @Test
    fun showsLaunchAfterInstallCheckboxEnabledByDefault() {
        val rendered =
            renderWindowsInstallerScript(
                template =
                    """
                    [Tasks]
                    Name: "desktopicon"; Description: "Create a &desktop shortcut"; Flags: checkedonce

                    [Run]
                    Filename: "{app}\{{ launcherExecutableName }}"; Description: "{cm:LaunchProgram,Technic Launcher}"; Flags: nowait postinstall skipifsilent
                    """.trimIndent(),
                metadata = WindowsInstallerMetadata(),
            )

        assertTrue(
            rendered.contains(
                "Filename: \"{app}\\launcher.exe\"; Description: \"{cm:LaunchProgram,Technic Launcher}\"; Flags: nowait postinstall skipifsilent",
            ),
        )
        assertFalse(rendered.contains("unchecked"))
        assertTrue(rendered.contains("Name: \"desktopicon\""))
        assertTrue(rendered.contains("Flags: checkedonce"))
    }

    @Test
    fun zeroOrNegativeInstallerJreMajorVersionFailsFast() {
        val previousValue = System.getProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY")

        try {
            System.setProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY", "0")

            val zeroProject = ProjectBuilder.builder().build()
            assertThrows(GradleException::class.java) {
                zeroProject.windowsInstallerMetadata()
            }

            System.setProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY", "-1")

            val negativeProject = ProjectBuilder.builder().build()
            assertThrows(GradleException::class.java) {
                negativeProject.windowsInstallerMetadata()
            }
        } finally {
            if (previousValue == null) {
                System.clearProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY")
            } else {
                System.setProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY", previousValue)
            }
        }
    }

    @Test
    fun malformedProjectPropertyFailsFast() {
        val previousValue = System.getProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY")
        System.setProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY", "twenty-five")

        try {
            val project = ProjectBuilder.builder().build()

            assertThrows(GradleException::class.java) {
                project.windowsInstallerMetadata()
            }
        } finally {
            if (previousValue == null) {
                System.clearProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY")
            } else {
                System.setProperty("org.gradle.project.$WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY", previousValue)
            }
        }
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
