import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.nio.charset.StandardCharsets

plugins {
    id("com.diffplug.spotless") version "8.4.0"
    id("net.technicpack.launcher-packaging")
    alias(libs.plugins.sentry.jvm)
}

group = "net.technicpack"
val buildNumber = providers.environmentVariable("BUILD_NUMBER").orElse("0").get()
version = "4.0-$buildNumber"
val runtimeJavaVersion = JavaLanguageVersion.of(25)
val targetJavaRelease = 8

repositories {
    mavenCentral()
}

spotless {
    java {
        target("src/*/java/**/*.java")
        googleJavaFormat()
    }

    kotlin {
        target("buildSrc/src/**/*.kt")
        ktlint()
    }

    kotlinGradle {
        target("*.gradle.kts", "buildSrc/*.gradle.kts")
        ktlint()
    }
}

java {
    toolchain {
        languageVersion = runtimeJavaVersion
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = StandardCharsets.UTF_8.name()
    options.compilerArgs.add("-Xlint:all")
    options.isDeprecation = true
    options.isWarnings = true
}

tasks.named<JavaCompile>("compileJava") {
    options.release.set(targetJavaRelease)
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

val sentryAuthToken = providers.environmentVariable("SENTRY_AUTH_TOKEN").orNull

sentry {
    // Keep dependency graph controlled in our own build script.
    autoInstallation.enabled.set(false)
    includeDependenciesReport.set(false)

    // Match old Maven behavior: upload source bundles only when auth token is available.
    includeSourceContext.set(sentryAuthToken != null)

    if (sentryAuthToken != null) {
        org.set("technic")
        projectName.set("launcher")
        url.set("https://sentry.technicpack.net/")
        authToken.set(sentryAuthToken)
    }
}

tasks.test {
    useJUnitPlatform()
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion = runtimeJavaVersion
        },
    )
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}
