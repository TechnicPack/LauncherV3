plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.4.2")
    implementation("edu.sc.seis.launch4j:launch4j:4.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.0")
}

tasks.test {
    useJUnitPlatform()
}
