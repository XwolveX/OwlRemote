import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.file.DuplicatesStrategy

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
    implementation("org.bytedeco:javacv-platform:1.5.10")
    implementation("org.bytedeco:javacv:1.5.10")
    implementation("org.bytedeco:ffmpeg-platform:6.1-1.5.10")
}
application {
    mainClass.set("MainLauncher")
}
tasks.withType<Jar>().configureEach {
    manifest {
        attributes["Main-Class"] = "MainLauncher"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
        exclude("META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/license/**")
        exclude("META-INF/NOTICE", "META-INF/NOTICE.txt", "META-INF/notice/**")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/INDEX.LIST")
        exclude("module-info.class")
    }
}


tasks.test {
    useJUnitPlatform()
}

// Optional: Ensure UTF-8 encoding
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}