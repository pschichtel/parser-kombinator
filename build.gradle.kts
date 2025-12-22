import io.github.danielliu1123.deployer.PublishingType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import pl.allegro.tech.build.axion.release.domain.PredefinedVersionCreator

plugins {
    signing
    `maven-publish`
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.mavenDeployer)
    alias(libs.plugins.detekt)
    alias(libs.plugins.axionRelease)
}

scmVersion {
    tag {
        prefix = ""
    }
    nextVersion {
        suffix = "SNAPSHOT"
        separator = "-"
    }
    versionCreator = PredefinedVersionCreator.SIMPLE.versionCreator
}

group = "tel.schich"
version = scmVersion.version
val isSnapshot = version.toString().endsWith("-SNAPSHOT")
val snapshotsRepo = "mavenCentralSnapshots"
val releasesRepo = "mavenLocal"

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
        this.
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

publishing {
    repositories {
        maven {
            name = snapshotsRepo
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            credentials(PasswordCredentials::class)
        }
        maven {
            name = releasesRepo
            url = layout.buildDirectory.dir("repo").get().asFile.toURI()
        }
    }
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

val ci = System.getenv("CI") != null
private val signingKey = System.getenv("SIGNING_KEY")?.ifBlank { null }?.trim()
private val signingKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")?.ifBlank { null }?.trim() ?: ""

when {
    signingKey != null -> {
        logger.lifecycle("Received a signing key, using in-memory pgp keys!")
        signing {
            useInMemoryPgpKeys(signingKey, signingKeyPassword)
            sign(publishing.publications)
        }
    }
    !ci -> {
        logger.lifecycle("Not running in CI, using the gpg command!")
        signing {
            useGpgCmd()
            sign(publishing.publications)
        }
    }
    else -> {
        logger.lifecycle("Not signing artifacts!")
    }
}

private fun Project.getSecret(name: String): Provider<String> = provider {
    val env = System.getenv(name)
        ?.ifBlank { null }
    if (env != null) {
        return@provider env
    }

    val propName = name.split("_")
        .map { it.lowercase() }
        .joinToString(separator = "") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
        .replaceFirstChar { it.lowercase() }

    property(propName) as String
}

deploy {
    // dirs to upload, they will all be packaged into one bundle
    dirs = provider {
        allprojects
            .map { it.layout.buildDirectory.dir("repo").get().asFile }
            .filter { it.exists() }
            .toList()
    }
    username = project.getSecret("MAVEN_CENTRAL_PORTAL_USERNAME")
    password = project.getSecret("MAVEN_CENTRAL_PORTAL_PASSWORD")
    publishingType = if (ci) {
        PublishingType.WAIT_FOR_PUBLISHED
    } else {
        PublishingType.USER_MANAGED
    }
}

tasks.deploy {
    for (project in allprojects) {
        val publishTasks = project.tasks
            .withType<PublishToMavenRepository>()
        mustRunAfter(publishTasks)
    }
}

val mavenCentralDeploy by tasks.registering(DefaultTask::class) {
    group = "publishing"

    val repo = if (isSnapshot) {
        snapshotsRepo
    } else {
        dependsOn(tasks.deploy)
        releasesRepo
    }
    for (project in allprojects) {
        val publishTasks = project.tasks
            .withType<PublishToMavenRepository>()
            .matching { it.repository.name == repo }
        dependsOn(publishTasks)
    }

    doFirst {
        if (isSnapshot) {
            logger.lifecycle("Snapshot deployment!")
        } else {
            logger.lifecycle("Release deployment!")
        }
    }
}
