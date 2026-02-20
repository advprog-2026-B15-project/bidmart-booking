import org.gradle.api.plugins.quality.Checkstyle

plugins {
	java
	checkstyle
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
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("org.postgresql:postgresql")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
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
