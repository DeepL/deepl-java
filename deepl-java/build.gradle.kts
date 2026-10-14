import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "6.25.0"
}

group = "com.deepl.api"
version = "1.17.0"

val sharedManifest = the<JavaPluginConvention>().manifest {
    attributes (
        "Implementation-Title" to "Gradle",
        "Implementation-Version" to version
    )
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:20.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-inline:4.11.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.14") { because("java.net.HttpURLConnection does not support PATCH") }

//    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("com.google.code.gson:gson:2.10.1")
}


tasks.named<Test>("test") {
    useJUnitPlatform()
}

spotless {
    java {
        googleJavaFormat("1.7")
        removeUnusedImports()
    }
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allJava)
    manifest = project.the<JavaPluginConvention>().manifest {
        from(sharedManifest)
    }
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc.get().destinationDir)
    manifest = project.the<JavaPluginConvention>().manifest {
        from(sharedManifest)
    }
}

publishing {
    repositories {
        maven {
            val mavenUploadUsername: String? by project
            val mavenUploadPassword: String? by project
            name = "MavenCentral"
            val releasesRepoUrl = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://central.sonatype.com/repository/maven-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = mavenUploadUsername
                password = mavenUploadPassword
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            pom {
                name.set("deepl-java")
                description.set("DeepL API Java Client Library")
                url.set("https://www.github.com/DeepLcom/deepl-java")
                properties.set(mapOf(
                    "java.version" to "1.8",
                    "project.build.sourceEncoding" to "UTF-8",
                    "project.reporting.outputEncoding" to "UTF-8"
                ))
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://www.opensource.org/licenses/mit-license.php")
                    }
                }
                developers {
                    developer {
                        id.set("deepl")
                        name.set("DeepL SE")
                        email.set("open-source@deepl.com")
                    }
                }
                organization {
                    name.set("DeepL SE")
                    url.set("https://www.deepl.com")
                }
                scm {
                    connection.set("scm:git:git://github.com/DeepLcom/deepl-java.git")
                    developerConnection.set("scm:git:ssh://github.com/DeepLcom/deepl-java.git")
                    url.set("https://www.github.com/DeepLcom/deepl-java")
                }
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}

tasks.register("triggerDeployment") {
    doLast {
        val mavenUploadUsername: String? by project
        val mavenUploadPassword: String? by project
        val namespace = "com.deepl.api"
        val url = "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$namespace?publishing_type=automatic"

        println("Triggering deployment for namespace: $namespace")
        println("Endpoint: $url")

        val credentials = "$mavenUploadUsername:$mavenUploadPassword"
        val encodedCredentials = Base64.getEncoder().encodeToString(credentials.toByteArray())

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $encodedCredentials")
        connection.doOutput = true
        connection.outputStream.write(byteArrayOf())

        val responseCode = connection.responseCode
        val response = if (responseCode == 200) {
            connection.inputStream.bufferedReader().readText()
        } else {
            connection.errorStream?.bufferedReader()?.readText() ?: "No error response"
        }

        println("Response code: $responseCode")
        if (response.isNotBlank()) {
            println("Response body: $response")
        }

        if (responseCode != 200) {
            throw GradleException("Failed to trigger deployment: HTTP $responseCode - $response")
        }

        println("Deployment triggered successfully")
    }
}

