plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
    checkstyle
    jacoco
    id("info.solidsoft.pitest") version "1.15.0"
}

group = "de.thomasuebel"
version = "1.0.7-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    implementation("org.bstats:bstats-bukkit:3.0.2")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        relocate("org.bstats", "de.thomasuebel.lastactiveplayers.libs.bstats")
        relocate("org.xerial", "de.thomasuebel.lastactiveplayers.libs.sqlite")
        archiveClassifier = ""
        minimize()
    }

    assemble {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)
        // Exclude the same Bukkit-framework classes excluded from pitest: thin wiring over
        // domain objects that cannot be unit-tested without MockBukkit.
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "de/thomasuebel/lastactiveplayers/LastActivePlayers.class",
                        "de/thomasuebel/lastactiveplayers/listener/**",
                        "de/thomasuebel/lastactiveplayers/session/BukkitHeartbeat.class"
                    )
                }
            })
        )
        reports {
            xml.required = true
            html.required = true
        }
    }
}

checkstyle {
    toolVersion = "10.20.2"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isShowViolations = true
}

pitest {
    junit5PluginVersion.set("1.2.1")
    targetClasses.set(setOf("de.thomasuebel.lastactiveplayers.*"))
    targetTests.set(setOf("de.thomasuebel.lastactiveplayers.*"))
    outputFormats.set(setOf("HTML", "XML"))
    mutationThreshold.set(70)
    threads.set(4)
    // Exclude mutations on java.sql.Connection and PreparedStatement methods whose
    // removal produces equivalent mutants: JDBC auto-commits on setAutoCommit(true),
    // making explicit commit() and setAutoCommit(false) calls indistinguishable
    // under test; SQLite JDBC treats unset PreparedStatement parameters as NULL,
    // making setNull() equivalent to leaving the parameter unset.
    avoidCallsTo.set(setOf("java.sql.Connection", "java.sql.PreparedStatement"))
    // Exclude Bukkit-framework classes (plugin main, listeners, scheduled tasks) from mutation
    // targets. These classes are thin wiring over domain objects that are fully covered;
    // unit-testing them would require MockBukkit (a Paper integration-test framework) which
    // is not yet in scope for this project.
    excludedClasses.set(setOf(
        "de.thomasuebel.lastactiveplayers.LastActivePlayers",
        "de.thomasuebel.lastactiveplayers.listener.*",
        "de.thomasuebel.lastactiveplayers.session.BukkitHeartbeat"
    ))
}
