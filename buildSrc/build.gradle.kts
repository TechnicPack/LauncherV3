plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.10")
    implementation("edu.sc.seis.launch4j:launch4j:4.0.0")
}
