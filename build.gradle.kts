plugins {
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.dokka") version "2.1.0"
    `maven-publish`
    signing
}

group = "com.eigenity"
version = "1.0.0"

repositories { mavenCentral() }

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-Xjvm-default=all") // nicer default methods for interfaces
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

// Generate Javadoc-style docs with Dokka and publish as a jar
val dokkaJavadoc by tasks.existing(org.jetbrains.dokka.gradle.DokkaTask::class)
val javadocJar by tasks.registering(Jar::class) {
    dependsOn(dokkaJavadoc)
    from(dokkaJavadoc.map { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

// ---- Publishing (creates a single Maven publication with sources + javadoc) ----
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(javadocJar)

            pom {
                name.set("your-lib")
                description.set("Pure Kotlin JVM library.")
                url.set("https://github.com/youruser/your-lib")

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                scm {
                    url.set("https://github.com/youruser/your-lib")
                    connection.set("scm:git:https://github.com/youruser/your-lib.git")
                    developerConnection.set("scm:git:ssh://git@github.com/youruser/your-lib.git")
                }
                developers {
                    developer {
                        id.set("youruser")
                        name.set("Your Name")
                        url.set("https://github.com/youruser")
                    }
                }
            }
        }
    }
    // Repos: publish to local for testing; upload to Central happens via the Portal/JReleaser (see notes).
    repositories {
        maven { name = "localStaging"; url = uri(layout.buildDirectory.dir("staging-repo")) }
    }
}

signing {
    // Recommended: in-memory PGP keys via gradle.properties or CI env vars
    // ORG_GRADLE_PROJECT_signingKey, ORG_GRADLE_PROJECT_signingPassword
    sign(publishing.publications["mavenJava"])
}
