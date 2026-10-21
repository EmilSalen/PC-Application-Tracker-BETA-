import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.20"
    id("org.jetbrains.compose") version "1.12.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20"
}

group = "com.example.timekeeper"
version = "1.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("net.java.dev.jna:jna:5.19.1")
    implementation("net.java.dev.jna:jna-platform:5.19.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.slf4j:slf4j-nop:2.0.16")
}

compose.desktop {
    application {
        mainClass = "com.example.timekeeper.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "TimeKeeper"
            packageVersion = "1.0.3"
            description = "PC screen-time tracker"
            vendor = "Emil Joy Salen"

            // The JDK packs only the modules it can see statically referenced.
            // sqlite-jdbc loads java.sql.DriverManager by name at runtime, so the
            // analyzer misses it and the app dies with "NoClassDefFoundError:
            // java/sql/DriverManager" the moment it opens the database. Naming the
            // module here forces it into the bundled runtime.
            modules("java.sql")
            windows {
                menu = true
                shortcut = true
                // Fixed UUID so reinstalls replace the same entry instead of duplicating.
                upgradeUuid = "E7B2B0F6-4C12-4E8B-9C2E-1D6A0A9F3C21"
                iconFile.set(project.file("icons/app.ico"))
            }
        }
    }
}
