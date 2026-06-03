package org.apereo.cas.gateway.routing;

import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.configuration.support.RequiresModule;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiresModule(name = "cas-server-support-unified-gateway")
public class GatewayRoutingProperties implements CasFeatureModule, Serializable {

    @Serial
    private static final long serialVersionUID = 8581975789429803862L;

    private Map<String, String> endpointOverrides = new LinkedHashMap<>();

    public Map<String, String> getEndpointOverrides() {
        return endpointOverrides;
    }

    public void setEndpointOverrides(final Map<String, String> endpointOverrides) {
        this.endpointOverrides = endpointOverrides;
    }
}