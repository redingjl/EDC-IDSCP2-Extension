package de.fhg.ids.edcidscp2;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Provides support for reading data from an IDSCP2 endpoint and sending data to an IDSCP2 endpoint.
 */
@Extension(value = Idscp2Extension.NAME)
public class Idscp2Extension implements ServiceExtension {
    public static final String NAME = "IDSCP2 Extension";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        monitor.info("###############IDSCP2 Extension started###############");
    }
}
