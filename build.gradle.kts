import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    signing
    java
    `maven-publish`
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    id("io.github.gradle-nexus.publish-plugin")
    id("io.gitlab.arturbosch.detekt")
}

group = "tel.schich"
version = "0.3.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

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
    jvm {
        withJava()
    }
    js(IR) {
        browser {
            binaries.executable()
        }
        nodejs()
    }
    sourceSets {
        val commonMain by getting {}

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
        }

        val jsMain by getting {
        }

        getByName("jvmTest") {
            dependencies {
                val junitVersion = "5.9.0"
                implementation(kotlin("test"))
                implementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
                implementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
                implementation("org.slf4j:slf4j-simple:1.7.36")
            }
        }

        getByName("jsTest") {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

val javadocJar by tasks.creating(Jar::class) {
    from(tasks.dokkaHtml)
    dependsOn(tasks.dokkaHtml)
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
