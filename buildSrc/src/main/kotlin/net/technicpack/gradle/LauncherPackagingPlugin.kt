package net.technicpack.gradle

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import edu.sc.seis.launch4j.Launch4jPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import java.nio.charset.StandardCharsets

class LauncherPackagingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            pluginManager.apply("java")
            pluginManager.apply("application")
            pluginManager.apply("com.gradleup.shadow")
            pluginManager.apply("edu.sc.seis.launch4j")

            val buildNumber = providers.environmentVariable("BUILD_NUMBER").orElse("0").get()

            extensions.configure<JavaApplication> {
                mainClass.set("net.technicpack.launcher.LauncherMain")
            }

            extensions.getByType<SourceSetContainer>().named("main") {
                resources.setSrcDirs(emptyList<String>())
            }

            tasks.named<ProcessResources>("processResources") {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                from(layout.projectDirectory.file("LICENSE.txt"))

                from(layout.projectDirectory.dir("src/main/resources")) {
                    include("**/*")
                    exclude("app/**", "exe/**", "version")
                    into("net/technicpack/launcher/resources")
                }

                from(layout.projectDirectory.file("src/main/resources/lang/UIText_en.properties")) {
                    into("net/technicpack/launcher/resources/lang")
                    rename { "UIText.properties" }
                }

                from(layout.projectDirectory.dir("src/main/resources")) {
                    include("version")
                    into("net/technicpack/launcher/resources")
                    filteringCharset = StandardCharsets.UTF_8.name()
                    expand(mapOf("buildNumber" to buildNumber))
                }
            }

            tasks.named<ShadowJar>("shadowJar") {
                archiveClassifier.set("")

                minimize {
                    exclude(dependency("org.ccil.cowan.tagsoup:.*:.*"))
                    exclude(project(":"))
                }

                exclude(
                    "META-INF/*.txt",
                    "META-INF/info.xml",
                    "META-INF/ASL2.0",
                    "META-INF/LICENSE",
                    "META-INF/NOTICE"
                )

                manifest {
                    attributes["Main-Class"] = "net.technicpack.launcher.LauncherMain"
                }
            }

            tasks.named<Jar>("jar") {
                enabled = false
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

                bundledJrePath.set("%JAVA_HOME%;%PATH%")
                jreMinVersion.set("1.8.0")

                jvmOptions.set(
                    setOf(
                        "-Djava.net.preferIPv4Stack=true",
                        "-Dawt.useSystemAAFontSettings=lcd",
                        "-Dswing.aatext=true"
                    )
                )

                version.set("4.0.0.$buildNumber")
                textVersion.set("4.0.0.$buildNumber")
                fileDescription.set("Technic Launcher")
                productName.set("Technic Launcher")
                internalName.set("launcher")
                copyright.set("Syndicate, LLC, https://www.technicpack.net")
            }

            tasks.named("createExe") {
                dependsOn(tasks.named("shadowJar"))
            }

            val packageOsx = tasks.register<Zip>("packageOsx") {
                dependsOn(tasks.named("shadowJar"))
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

            val signShadowJar = tasks.register<Exec>("signShadowJar") {
                dependsOn(tasks.named("shadowJar"))
                onlyIf { System.getenv("CERT_KEYSTORE") != null }

                doFirst {
                    fun requiredEnv(name: String): String {
                        return System.getenv(name)
                            ?: throw org.gradle.api.GradleException("Missing required env var for signing: $name")
                    }

                    val certKeystore = requiredEnv("CERT_KEYSTORE")
                    val certAlias = requiredEnv("CERT_ALIAS")
                    val certStorePass = requiredEnv("CERT_STOREPASS")
                    val certKeyPass = requiredEnv("CERT_KEYPASS")
                    val jarPath = tasks.named<ShadowJar>("shadowJar").get().archiveFile.get().asFile.absolutePath

                    commandLine(
                        "jarsigner",
                        "-verbose",
                        "-storetype", "pkcs12",
                        "-keystore", certKeystore,
                        "-storepass", certStorePass,
                        "-keypass", certKeyPass,
                        jarPath,
                        certAlias
                    )
                }
            }

            tasks.register("package") {
                group = "build"
                description = "Builds fat JAR, Windows EXE, and macOS app zip."
                dependsOn(tasks.named("shadowJar"), tasks.named("createExe"), packageOsx, signShadowJar)
            }
        }
    }
}
