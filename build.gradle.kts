plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.json:json:20231013")
    implementation("com.formdev:flatlaf:3.2.5")
    implementation("com.formdev:flatlaf-extras:3.2.5")
}
application {
    mainClass.set("MainLauncher")
}
tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainLauncher"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

tasks.test {
    useJUnitPlatform()
}