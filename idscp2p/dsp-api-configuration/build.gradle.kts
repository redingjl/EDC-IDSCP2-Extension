/*
*  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
*
*  This program and the accompanying materials are made available under the
*  terms of the Apache License, Version 2.0 which is available at
*  https://www.apache.org/licenses/LICENSE-2.0
*
*  SPDX-License-Identifier: Apache-2.0
*
*  Contributors:
*       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
*
*/

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
    // api(project(":spi:common:catalog-spi"))
    implementation("org.eclipse.edc:catalog-spi:0.1.0")
    //api(project(":spi:common:core-spi"))
    implementation("org.eclipse.edc:core-spi:0.1.0")
    //api(project(":extensions:common:http"))
    implementation("org.eclipse.edc:http:0.1.0")
    //api(project(":extensions:common:json-ld"))
    implementation("org.eclipse.edc:json-ld:0.1.0")
    api(project(":idscp2"))

    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")

    //testImplementation(project(":core:common:junit"))
}
