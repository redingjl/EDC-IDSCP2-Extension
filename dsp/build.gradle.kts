/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */


plugins {
    `java-library`
}

dependencies {
    api(project(":dsp:dsp-api-configuration"))
    api(project(":dsp:dsp-catalog"))
    api(project(":dsp:dsp-http-core"))
    api(project(":dsp:dsp-http-spi"))
    api(project(":dsp:dsp-negotiation"))
    api(project(":dsp:dsp-transfer-process"))
    api(project(":dsp:dsp-transform"))
}
