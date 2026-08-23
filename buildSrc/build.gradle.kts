plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // Force patched versions of build-time transitives pulled by the shadow
    // plugin (shadow 9.4.2 still drags in log4j-core 2.25.3 / plexus-utils
    // 4.0.2). Build tooling only — never shipped in the launcher — but this
    // clears the Dependabot alerts (GHSA-6fmv-xxpf-w3cw, log4j-core advisories).
    constraints {
        implementation("org.apache.logging.log4j:log4j-core:2.26.1")
        implementation("org.codehaus.plexus:plexus-utils:4.0.3")
    }

    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.4.3")
    implementation("edu.sc.seis.launch4j:launch4j:4.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.0")
}

tasks.test {
    useJUnitPlatform()
}
