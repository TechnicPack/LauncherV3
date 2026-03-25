package net.technicpack.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class LauncherPackagingPluginTest {
    @Test
    fun configuresLaunch4jToPreferBundledLauncherRuntimeBeforeExistingJava(
        @TempDir tempDir: Path,
    ) {
        val repositoryRoot = locateRepositoryRoot()
        val initScript = tempDir.resolve("assert-launch4j-runtime.gradle")

        Files.writeString(
            initScript,
            """
            gradle.afterProject { project ->
                if (project == project.rootProject) {
                    project.tasks.register("assertLaunch4jBundledJrePath") {
                        doLast {
                            def launch4j = project.extensions.getByName("launch4j")
                            def bundledJrePath = launch4j.bundledJrePath.get()

                            assert bundledJrePath == "launcher-runtime;%JAVA_HOME%;%PATH%" :
                                "Expected launcher-runtime;%JAVA_HOME%;%PATH%, got '${'$'}bundledJrePath'"
                        }
                    }
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(repositoryRoot.toFile())
                .withArguments("assertLaunch4jBundledJrePath", "-I", initScript.toString(), "--stacktrace")
                .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun doesNotRegisterGeneratedInstallerInputTask(
        @TempDir tempDir: Path,
    ) {
        val repositoryRoot = locateRepositoryRoot()
        val initScript = tempDir.resolve("assert-installer-task-layout.gradle")

        Files.writeString(
            initScript,
            """
            gradle.afterProject { project ->
                if (project == project.rootProject) {
                    project.tasks.register("assertInstallerTaskLayout") {
                        doLast {
                            assert project.tasks.findByName("generatedInstallerInput") == null : "generatedInstallerInput should not be registered"
                        }
                    }
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(repositoryRoot.toFile())
                .withArguments("assertInstallerTaskLayout", "-I", initScript.toString(), "--stacktrace")
                .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun aggregatePackageDoesNotDependOnWindowsInstaller(
        @TempDir tempDir: Path,
    ) {
        val repositoryRoot = locateRepositoryRoot()
        val initScript = tempDir.resolve("assert-package-task-layout.gradle")

        Files.writeString(
            initScript,
            """
            gradle.afterProject { project ->
                if (project == project.rootProject) {
                    project.tasks.register("assertAggregatePackageTaskLayout") {
                        doLast {
                            def packageTask = project.tasks.getByName("package")
                            def dependencyNames = packageTask.taskDependencies.getDependencies(packageTask)*.name

                            assert !dependencyNames.contains("packageWindowsInstaller") :
                                "package should not depend on packageWindowsInstaller"
                        }
                    }
                }
            }
            """.trimIndent(),
        )

        val result =
            GradleRunner
                .create()
                .withProjectDir(repositoryRoot.toFile())
                .withArguments("assertAggregatePackageTaskLayout", "-I", initScript.toString(), "--stacktrace")
                .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }

    private fun locateRepositoryRoot(): Path =
        generateSequence(Paths.get("").toAbsolutePath().normalize()) { it.parent }
            .firstOrNull { candidate ->
                Files.isRegularFile(candidate.resolve("settings.gradle.kts")) &&
                    Files.isRegularFile(candidate.resolve("build.gradle.kts")) &&
                    Files.isDirectory(candidate.resolve("buildSrc"))
            }
            ?: throw IllegalStateException("Could not locate repository root for launcher packaging integration test.")
}
