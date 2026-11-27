import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "6.25.0"
    jacoco
    `java-test-fixtures`
}

group = "com.deepl.api"
version = "2.0.0"

val sharedManifest = java.manifest {
    attributes (
        "Implementation-Title" to "Gradle",
        "Implementation-Version" to version
    )
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
// Gradle itself needs Java 17+; --release 8 also checks the code against the Java 8 API.
tasks.withType<JavaCompile>().configureEach { options.release.set(8) }

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:20.1.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.14") { because("java.net.HttpURLConnection does not support PATCH") }

//    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("com.google.code.gson:gson:2.10.1")
}


jacoco { toolVersion = "0.8.14" }

// offlineTest needs no key and no network; realApiTest needs DEEPL_AUTH_KEY and DEEPL_ADMIN_KEY.
testing {
    suites {
        register<JvmTestSuite>("offlineTest") {
            useJUnitJupiter("5.10.0")
            dependencies {
                implementation(project())
                // The last WireMock line that runs on Java 8.
                implementation("com.github.tomakehurst:wiremock-jre8:2.35.2")
                // Encodes the expected form bodies independently of the SDK's own encoder.
                implementation("org.apache.httpcomponents:httpclient:4.5.14")
            }
            targets.all {
                testTask.configure {
                    // Any request that does not go to the local WireMock server fails.
                    systemProperty("http.proxyHost", "127.0.0.1")
                    systemProperty("http.proxyPort", "9")
                    systemProperty("https.proxyHost", "127.0.0.1")
                    systemProperty("https.proxyPort", "9")
                }
            }
        }
        register<JvmTestSuite>("realApiTest") {
            useJUnitJupiter("5.10.0")
            dependencies {
                implementation(project())
                // Java 11+, which is why this is not part of offlineTest.
                implementation("io.github.littleproxy:littleproxy:2.9.1")
            }
        }
    }
}

// -PtestJavaHome=<JDK> runs the tests on that JVM, e.g. Java 8 or 11, which cannot run Gradle.
val testJavaHome = findProperty("testJavaHome") as String?
tasks.withType<Test>().configureEach {
    if (testJavaHome != null) executable = "$testJavaHome/bin/java"
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

dependencies {
    "offlineTestImplementation"(testFixtures(project(":deepl-java")))
    "realApiTestImplementation"(testFixtures(project(":deepl-java")))
}

tasks.named("check") {
    dependsOn(tasks.named("offlineTest"))
}

// The test fixtures are shared by the test suites only and must not be published.
components["java"].let { javaComponent ->
    javaComponent as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["testFixturesApiElements"]) { skip() }
    javaComponent.withVariantsFromConfiguration(configurations["testFixturesRuntimeElements"]) { skip() }
}

// The coverage report is written even when a test fails, so that CI can publish it.
listOf("offlineTest", "realApiTest").forEach { suite ->
    val coverage = tasks.register<JacocoReport>("${suite}Coverage") {
        executionData(tasks.named(suite).get())
        sourceSets(sourceSets.main.get())
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
    tasks.named(suite) { finalizedBy(coverage) }
}

spotless {
    java {
        googleJavaFormat("1.10.0")
        removeUnusedImports()
    }
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allJava)
    manifest = project.java.manifest {
        from(sharedManifest)
    }
}

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
    manifest = project.java.manifest {
        from(sharedManifest)
    }
}

publishing {
    repositories {
        maven {
            val mavenUploadUsername = findProperty("mavenUploadUsername") as String?
            val mavenUploadPassword = findProperty("mavenUploadPassword") as String?
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
                url.set("https://github.com/DeepL/deepl-java")
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
                    connection.set("scm:git:git://github.com/DeepL/deepl-java.git")
                    developerConnection.set("scm:git:ssh://github.com/DeepL/deepl-java.git")
                    url.set("https://github.com/DeepL/deepl-java")
                }
            }
        }
    }
}

signing {
    val signingKey = findProperty("signingKey") as String?
    val signingPassword = findProperty("signingPassword") as String?
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}

tasks.register("triggerDeployment") {
    doLast {
        val mavenUploadUsername = findProperty("mavenUploadUsername") as String?
        val mavenUploadPassword = findProperty("mavenUploadPassword") as String?
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

