import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.nio.charset.StandardCharsets

plugins {
    id("net.technicpack.launcher-packaging")
}

group = "net.technicpack"
val buildNumber = providers.environmentVariable("BUILD_NUMBER").orElse("0").get()
version = "4.0-$buildNumber"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = StandardCharsets.UTF_8.name()
    options.compilerArgs.add("-Xlint:all")
    options.isDeprecation = true
    options.isWarnings = true
}

dependencies {
    implementation(libs.joda.time)
    implementation(libs.jcommander)
    implementation(libs.gson)
    implementation(libs.commons.io)
    runtimeOnly(libs.tagsoup)
    implementation(libs.flying.saucer.core)
    implementation(libs.commons.lang3)
    implementation(libs.commons.text)
    implementation(libs.commons.compress)
    implementation(libs.zstd.jni)
    implementation(libs.xz)
    implementation(libs.commons.codec)
    implementation(libs.guava)
    implementation(libs.maven.artifact)
    implementation(libs.google.oauth.client)
    implementation(libs.google.oauth.client.jetty)
    implementation(libs.google.oauth.client.java6)
    implementation(libs.google.http.client)
    implementation(libs.google.http.client.apache.v5)
    implementation(libs.google.http.client.gson)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.nop)
    implementation(libs.sentry)
    implementation(libs.annotations)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(8)
    })
}
