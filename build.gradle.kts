import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

plugins {
	java
	checkstyle
	jacoco
	id("org.springframework.boot") version "3.5.11"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.sonarqube") version "6.3.1.5724"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "bidmart-booking"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.flywaydb:flyway-core")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
}

tasks.register<Test>("profileMilestone100") {
	group = "verification"
	description = "Runs milestone 100 integration profiling with Java Flight Recorder."
	extensions.configure<JacocoTaskExtension> {
		isEnabled = false
	}
	useJUnitPlatform()
	filter {
		includeTestsMatching("*.AuctionToNotificationFlowIntegrationTest")
		includeTestsMatching("*.NotificationStreamIntegrationTest")
		includeTestsMatching("*.DisputeApiIntegrationTest")
	}

	val outputDir = layout.buildDirectory.dir("profiling")
	doFirst {
		outputDir.get().asFile.mkdirs()
	}
	jvmArgs(
		"-XX:StartFlightRecording=filename=${outputDir.get().asFile}/milestone-100.jfr,"
			+ "settings=profile,dumponexit=true",
		"-XX:FlightRecorderOptions=stackdepth=128"
	)
}

jacoco {
	toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	classDirectories.setFrom(
		files(
			classDirectories.files.map {
				fileTree(it) {
					exclude("com/example/bidmartbooking/BidmartBookingApplication.class")
				}
			}
		)
	)
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)
	classDirectories.setFrom(tasks.jacocoTestReport.get().classDirectories)
	violationRules {
		rule {
			limit {
				counter = "LINE"
				value = "COVEREDRATIO"
				minimum = "0.90".toBigDecimal()
			}
			limit {
				counter = "BRANCH"
				value = "COVEREDRATIO"
				minimum = "0.90".toBigDecimal()
			}
		}
	}
}

tasks.check {
	dependsOn(tasks.jacocoTestCoverageVerification)
}

checkstyle {
	toolVersion = "10.18.2"
	configFile = file("config/checkstyle/checkstyle.xml")
	isIgnoreFailures = false
}

tasks.withType<Checkstyle> {
	reports {
		xml.required.set(true)
		html.required.set(true)
	}
}

sonar {
	properties {
		val sonarProjectKey =
			System.getenv("SONAR_PROJECT_KEY") ?: findProperty("sonarProjectKey")?.toString()
		val sonarOrganization =
			System.getenv("SONAR_ORGANIZATION") ?: findProperty("sonarOrganization")?.toString()

		if (!sonarProjectKey.isNullOrBlank()) {
			property("sonar.projectKey", sonarProjectKey)
		}
		if (!sonarOrganization.isNullOrBlank()) {
			property("sonar.organization", sonarOrganization)
		}
		property("sonar.host.url", "https://sonarcloud.io")
	}
}
