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

package org.eclipse.edc.protocol.dsp;

import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.dispatcher.DspHttpRemoteMessageDispatcherImpl;
import org.eclipse.edc.protocol.dsp.serialization.JsonLdRemoteMessageSerializerImpl;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpRemoteMessageDispatcher;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenDecorator;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

/**
 * Provides an implementation of {@link DspHttpRemoteMessageDispatcher} to support sending dataspace
 * protocol messages. The dispatcher can then be used by other extensions to add support for
 * specific message types.
 */
@Extension(value = DspHttpCoreExtension.NAME)
public class DspHttpCoreExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Core Extension";

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;
    @Inject
    private EdcHttpClient httpClient;
    @Inject
    private IdentityService identityService;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLd jsonLdService;
    @Inject(required = false)
    private TokenDecorator decorator;
    @Override
    public String name() {
        return NAME;
    }
    @Provider
    public DspHttpRemoteMessageDispatcher dspHttpRemoteMessageDispatcher(ServiceExtensionContext context) {
        TokenDecorator td; // either a decorator, or noop
        if (decorator != null) {
            td = decorator;
        } else {
            context.getMonitor().warning("No TokenDecorator was registered. The 'scope' field of outgoing protocol messages will be empty");
            td = bldr -> bldr;
        }
        var dispatcher = new DspHttpRemoteMessageDispatcherImpl(httpClient, identityService, td);
        dispatcherRegistry.register(dispatcher);
        return dispatcher;
    }

    @Provider
    public JsonLdRemoteMessageSerializer jsonLdRemoteMessageSerializer() {
        return new JsonLdRemoteMessageSerializerImpl(transformerRegistry, typeManager.getMapper(JSON_LD), jsonLdService);
    }

}
