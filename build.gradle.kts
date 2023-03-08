plugins {
    jacoco
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.21"
}

group = "Pupsen&Vupsen"
version = "3.0.0"
java.sourceCompatibility = JavaVersion.VERSION_16

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-devtools")

    runtimeOnly("com.h2database:h2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.beust:klaxon:5.6")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation(platform("org.junit:junit-bom:5.9.0"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

val jacocoExclude = listOf(
    "**/configuration/**",
    "**/entities/**",
    "**/enums/**",
    "**/repositories/**",
    "**/services/**",
    "**/*Application*"
)

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    useJUnitPlatform()
    maxHeapSize = "2G"
    extensions.configure<JacocoTaskExtension> {
        excludes = jacocoExclude
    }
    // Uncomment to run concurrent tests on your own PC
    /*jvmArgs(
        "-Xmx4096m",
        "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-exports", "java.base/jdk.internal.util=ALL-UNNAMED"
    )*/
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(false)
        csv.required.set(true)
        csv.outputLocation.set(file("${buildDir}/jacoco/report.csv"))
        html.outputLocation.set(file("${buildDir}/reports/jacoco"))
    }
    classDirectories.setFrom(classDirectories.files.map {
        fileTree(it).matching {
            exclude(jacocoExclude)
        }
    })
}