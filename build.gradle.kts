import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    signing
    `maven-publish`
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.nexusPublish)
    alias(libs.plugins.detekt)
}

group = "tel.schich"
version = "0.3.1"

tasks.withType<Test> {
    useJUnitPlatform()
    val traceProperty = "tel.schich.parser-kombinator.trace"
    systemProperties[traceProperty] = System.getProperty(traceProperty)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        freeCompilerArgs = listOf("-progressive")
    }
}

tasks.withType<Jar>().configureEach {
    metaInf.with(
        copySpec {
            from("${project.rootDir}/LICENSE")
        }
    )
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
    jvm {
    }
    js(IR) {
        browser {
            binaries.executable()
            testTask {
                useKarma {
                    useFirefoxHeadless()
                }
            }
        }
        nodejs()
    }
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
        }

        val jsMain by getting {
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    from(dokka.dokkaPublications.html.map { it.outputDirectory })
    dependsOn(tasks.dokkaGeneratePublicationHtml)
    archiveClassifier.set("javadoc")
}

fun isSnapshot() = version.toString().endsWith("-SNAPSHOT")

publishing {
    publications {
        publications.withType<MavenPublication> {
            pom {
                name.set("parser-kombinator")
                description.set("A simple parser combinator framework.")
                url.set("https://github.com/pschichtel/parser-kombinator")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("pschichtel")
                        name.set("Phillip Schichtel")
                        email.set("phillip@schich.tel")
                    }
                }
                scm {
                    url.set("https://github.com/pschichtel/kognigy")
                    connection.set("scm:git:https://github.com/pschichtel/parser-kombinator")
                    developerConnection.set("scm:git:git@github.com:pschichtel/parser-kombinator")
                }
            }
        }
        publications.named("jvm", MavenPublication::class) {
            artifact(javadocJar)
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

nexusPublishing {
    repositories {
        sonatype()
    }
}
