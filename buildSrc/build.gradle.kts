plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // Keep the existing build-tool minimum; Shadow 9.6.1 still requests Plexus 4.0.3.
    constraints {
        implementation("org.codehaus.plexus:plexus-utils:4.1.0")
    }

    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.6.1")
    implementation("edu.sc.seis.launch4j:launch4j:4.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

tasks.test {
    useJUnitPlatform()
}
