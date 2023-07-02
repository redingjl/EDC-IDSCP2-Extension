plugins {
    `java-library`
}

repositories {
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    mavenCentral()
}

dependencies {
    api(project(":idscp2p:dsp-api-configuration"))
}

/*plugins {
    id 'java'
}

group 'de.fhg.aisec.ids.edcidscp2'
version '0.1'

repositories {
    mavenCentral()
}

dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.8.1'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.8.1'
}

test {
    useJUnitPlatform()
}
*/