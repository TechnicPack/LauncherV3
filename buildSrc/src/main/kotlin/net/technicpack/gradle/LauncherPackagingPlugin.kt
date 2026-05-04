package net.technicpack.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import edu.sc.seis.launch4j.Launch4jPluginExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.jar.JarFile

private const val WINDOWS_INSTALLER_INPUT_DIRECTORY = "installer/windows"
private const val WINDOWS_INSTALLER_SCRIPT_FILE = "TechnicLauncherBootstrap.iss"
private const val WINDOWS_INSTALLER_DOCKER_IMAGE = "amake/innosetup"
private const val WINDOWS_INSTALLER_DOCKER_WORKDIR = "/work"

class LauncherPackagingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            pluginManager.apply("java")
            pluginManager.apply("application")
            pluginManager.apply("com.gradleup.shadow")
            pluginManager.apply("edu.sc.seis.launch4j")

            val buildNumber = providers.environmentVariable("BUILD_NUMBER").orElse("0").get()
            val buildYear =
                java.time.Year
                    .now()
                    .toString()

            extensions.configure<JavaApplication> {
                mainClass.set("net.technicpack.launcher.LauncherMain")
            }

            extensions.getByType<SourceSetContainer>().named("main") {
                resources.setSrcDirs(emptyList<String>())
            }

            tasks.named<ProcessResources>("processResources") {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                inputs.property("buildNumber", buildNumber)

                from(layout.projectDirectory.file("LICENSE.txt"))

                from(layout.projectDirectory.dir("src/main/resources")) {
                    include("**/*")
                    exclude("app/**", "exe/**", "version", "buildyear")
                    into("net/technicpack/launcher/resources")
                }

                from(layout.projectDirectory.file("src/main/resources/lang/UIText_en.properties")) {
                    into("net/technicpack/launcher/resources/lang")
                    rename { "UIText.properties" }
                }

                from(layout.projectDirectory.dir("src/main/resources")) {
                    include("version", "buildyear")
                    into("net/technicpack/launcher/resources")
                    filteringCharset = StandardCharsets.UTF_8.name()
                    expand(mapOf("buildNumber" to buildNumber, "buildYear" to buildYear))
                }
            }

            tasks.named<ShadowJar>("shadowJar") {
                archiveClassifier.set("")

                minimize {
                    exclude(dependency("org.ccil.cowan.tagsoup:.*:.*"))
                    // Keep SLF4J provider classes that are loaded via ServiceLoader.
                    exclude(dependency("org.slf4j:slf4j-nop:.*"))
                    exclude(project(":"))
                }

                exclude(
                    "META-INF/*.txt",
                    "META-INF/info.xml",
                    "META-INF/ASL2.0",
                    "META-INF/LICENSE",
                    "META-INF/NOTICE",
                )

                manifest {
                    attributes["Main-Class"] = "net.technicpack.launcher.LauncherMain"
                }
            }

            val shadowJarTask = tasks.named("shadowJar")
            val createExeTask = tasks.named("createExe")

            tasks.named<Jar>("jar") {
                enabled = false
            }

            val verifyShadowJarServices =
                tasks.register("verifyShadowJarServices") {
                    group = "verification"
                    description = "Verifies ServiceLoader providers referenced in the shadow jar exist."
                    dependsOn(shadowJarTask)
                    // createExe and shadowJar both write to build/libs; enforce ordering for Gradle 9 validation.
                    mustRunAfter(createExeTask)

                    val shadowJarArchive = tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile }
                    inputs.file(shadowJarArchive)

                    doLast {
                        val shadowJarFile = shadowJarArchive.get().asFile

                        JarFile(shadowJarFile).use { jar ->
                            val entryNames =
                                jar
                                    .entries()
                                    .asSequence()
                                    .map { it.name }
                                    .toSet()

                            val serviceEntries =
                                entryNames
                                    .asSequence()
                                    .filter { it.startsWith("META-INF/services/") && !it.endsWith("/") }
                                    .toList()

                            val missingProviders = mutableListOf<String>()

                            for (serviceEntry in serviceEntries) {
                                val serviceFile = jar.getJarEntry(serviceEntry) ?: continue
                                val providers =
                                    jar
                                        .getInputStream(serviceFile)
                                        .bufferedReader(StandardCharsets.UTF_8)
                                        .use { reader ->
                                            reader
                                                .lineSequence()
                                                .map { it.substringBefore('#').trim() }
                                                .filter { it.isNotEmpty() }
                                                .toList()
                                        }

                                for (provider in providers) {
                                    val providerClassPath = provider.replace('.', '/') + ".class"
                                    if (!entryNames.contains(providerClassPath)) {
                                        missingProviders += "$serviceEntry -> $provider"
                                    }
                                }
                            }

                            if (missingProviders.isNotEmpty()) {
                                throw GradleException(
                                    "Shadow jar has unresolved ServiceLoader providers:\n" +
                                        missingProviders.joinToString("\n"),
                                )
                            }
                        }
                    }
                }

            tasks.named("check") {
                dependsOn(verifyShadowJarServices)
            }

            tasks.named("startScripts") {
                dependsOn(shadowJarTask)
                dependsOn(createExeTask)
            }

            tasks.named("distTar") {
                dependsOn(shadowJarTask)
                dependsOn(createExeTask)
            }

            tasks.named("distZip") {
                dependsOn(shadowJarTask)
                dependsOn(createExeTask)
            }

            tasks.named("startShadowScripts") {
                dependsOn(createExeTask)
            }

            tasks.named("shadowDistTar") {
                dependsOn(createExeTask)
            }

            tasks.named("shadowDistZip") {
                dependsOn(createExeTask)
            }

            extensions.configure<Launch4jPluginExtension> {
                headerType.set("gui")
                stayAlive.set(false)
                mainClassName.set("net.technicpack.launcher.LauncherMain")
                setJarTask(tasks.named("shadowJar"))
                copyConfigurable.set(emptyList<String>())
                outputDir.set("libs")
                outfile.set(providers.provider { "launcher-${project.version}.exe" })
                icon.set("${project.projectDir}/src/main/resources/exe/icon.ico")

                bundledJrePath.set("launcher-runtime;%JAVA_HOME%;%PATH%")
                jreMinVersion.set("1.8.0")

                jvmOptions.set(
                    setOf(
                        "-Djava.net.preferIPv4Stack=true",
                        "-Dawt.useSystemAAFontSettings=lcd",
                        "-Dswing.aatext=true",
                    ),
                )

                version.set("4.0.0.$buildNumber")
                textVersion.set("4.0.0.$buildNumber")
                fileDescription.set("Technic Launcher")
                productName.set("Technic Launcher")
                internalName.set("launcher")
                copyright.set("Syndicate, LLC, https://www.technicpack.net")
            }

            val generatedInstallerDir = layout.buildDirectory.dir(WINDOWS_INSTALLER_INPUT_DIRECTORY)

            tasks.named("createExe") {
                dependsOn(shadowJarTask)
            }

            val generatedWindowsInstallerScript = generatedInstallerDir.map { it.file(WINDOWS_INSTALLER_SCRIPT_FILE) }

            val generateWindowsInstallerScriptTask =
                tasks.register("generateWindowsInstallerScript") {
                    group = "build"
                    description = "Renders the Inno Setup bootstrap installer script."

                    val templateFile = layout.projectDirectory.file(WINDOWS_INSTALLER_SCRIPT_TEMPLATE_PATH)

                    inputs.file(templateFile)
                    inputs.property("launcherDownloadUrl", WINDOWS_INSTALLER_STABLE_LAUNCHER_DOWNLOAD_URL)
                    inputs.property(
                        "windowsInstallerJreMajorVersion",
                        providers
                            .gradleProperty(WINDOWS_INSTALLER_JRE_MAJOR_VERSION_PROPERTY)
                            .orElse(DEFAULT_WINDOWS_INSTALLER_JRE_MAJOR_VERSION.toString()),
                    )
                    outputs.file(generatedWindowsInstallerScript)

                    doLast {
                        val metadata = windowsInstallerMetadata()
                        val outputFile = generatedWindowsInstallerScript.get().asFile

                        outputFile.parentFile.mkdirs()
                        outputFile.writeText(renderWindowsInstallerScript(metadata))
                    }
                }

            val packageWindowsInstaller =
                tasks.register<Exec>("packageWindowsInstaller") {
                    group = "build"
                    description = "Builds the Windows bootstrap installer with Inno Setup in Docker."
                    dependsOn(generateWindowsInstallerScriptTask)

                    val installerScript = generatedWindowsInstallerScript
                    val installerOutput =
                        layout.buildDirectory.file(
                            providers.provider {
                                buildWindowsInstallerOutputRelativePath()
                            },
                        )

                    inputs.file(installerScript)
                    outputs.file(installerOutput)

                    doFirst {
                        installerOutput
                            .get()
                            .asFile.parentFile
                            .mkdirs()
                        commandLine(
                            buildWindowsInstallerCommandLine(
                                projectDir = project.projectDir.absolutePath,
                                installerScriptPath = installerScript.get().asFile.absolutePath,
                                installerOutputDir =
                                    installerOutput
                                        .get()
                                        .asFile.parentFile.absolutePath,
                            ),
                        )
                    }
                }

            val packageOsx =
                tasks.register<Zip>("packageOsx") {
                    dependsOn(shadowJarTask)
                    mustRunAfter(tasks.named("createExe"))

                    archiveBaseName.set("launcher")
                    archiveVersion.set(providers.provider { project.version.toString() })
                    archiveClassifier.set("osx.app")
                    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

                    from(layout.projectDirectory.dir("src/main/app")) {
                        into("Technic.app")
                    }

                    from(tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile }) {
                        into("Technic.app/Contents/Java")
                        rename { "TechnicLauncher.jar" }
                    }

                    eachFile {
                        if (path == "Technic.app/Contents/MacOS/JavaAppLauncher") {
                            permissions {
                                user {
                                    read = true
                                    write = true
                                    execute = true
                                }
                                group {
                                    read = true
                                    write = false
                                    execute = true
                                }
                                other {
                                    read = true
                                    write = false
                                    execute = true
                                }
                            }
                        }
                    }
                }

            val signShadowJar =
                tasks.register<Exec>("signShadowJar") {
                    dependsOn(shadowJarTask)
                    onlyIf { System.getenv("CERT_KEYSTORE") != null }

                    doFirst {
                        fun requiredEnv(name: String): String =
                            System.getenv(name)
                                ?: throw org.gradle.api.GradleException("Missing required env var for signing: $name")

                        val certKeystore = requiredEnv("CERT_KEYSTORE")
                        val certAlias = requiredEnv("CERT_ALIAS")
                        val certStorePass = requiredEnv("CERT_STOREPASS")
                        val certKeyPass = requiredEnv("CERT_KEYPASS")
                        val jarPath =
                            tasks
                                .named<ShadowJar>("shadowJar")
                                .get()
                                .archiveFile
                                .get()
                                .asFile.absolutePath

                        commandLine(
                            "jarsigner",
                            "-verbose",
                            "-storetype",
                            "pkcs12",
                            "-keystore",
                            certKeystore,
                            "-storepass",
                            certStorePass,
                            "-keypass",
                            certKeyPass,
                            jarPath,
                            certAlias,
                        )
                    }
                }

            tasks.register("package") {
                group = "build"
                description = "Builds fat JAR, Windows EXE, and macOS app zip."
                dependsOn(shadowJarTask, tasks.named("createExe"), packageOsx, signShadowJar)
            }
        }
    }
}

internal fun buildWindowsInstallerCommandLine(
    projectDir: String,
    installerScriptPath: String,
    installerOutputDir: String,
): List<String> {
    val normalizedProjectDir = Path.of(projectDir).toAbsolutePath().normalize()
    val scriptRelativePath =
        normalizedProjectDir
            .relativize(Path.of(installerScriptPath).toAbsolutePath().normalize())
            .toString()
            .replace(File.separatorChar, '/')
    val outputRelativePath =
        normalizedProjectDir
            .relativize(Path.of(installerOutputDir).toAbsolutePath().normalize())
            .toString()
            .replace(File.separatorChar, '/')

    return listOf(
        "docker",
        "run",
        "--rm",
        "-i",
        "-v",
        "${normalizedProjectDir.toString().replace('\\', '/')}:$WINDOWS_INSTALLER_DOCKER_WORKDIR",
        WINDOWS_INSTALLER_DOCKER_IMAGE,
        "/Qp",
        "/O$outputRelativePath",
        "/FTechnicLauncherBootstrap",
        scriptRelativePath,
    )
}

internal fun buildWindowsInstallerOutputRelativePath(): String = "distributions/TechnicLauncherBootstrap.exe"
