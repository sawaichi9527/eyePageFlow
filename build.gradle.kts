plugins {
    kotlin("jvm") version "2.4.10"
    application
}

group = "io.github.sawaichi9527.eyepageflow"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

application {
    mainClass.set("io.github.sawaichi9527.eyepageflow.MainKt")
}

dependencies {
    implementation("com.formdev:flatlaf:3.7.2")
    implementation("com.fifesoft:rsyntaxtextarea:3.6.3")

    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}
