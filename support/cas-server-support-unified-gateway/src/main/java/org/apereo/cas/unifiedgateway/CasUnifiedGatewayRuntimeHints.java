package org.apereo.cas.unifiedgateway;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Runtime hints for unified gateway.
 *
 * @author CAS Contributor
 * @since 7.1.0
 */
public class CasUnifiedGatewayRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        hints.reflection()
                .registerType(AutoProtocol.class, MemberCategory.values())
                .registerType(ProtocolRouter.class, MemberCategory.values())
                .registerType(GatewayMfaProvider.class, MemberCategory.values())
                .registerType(TicketStorage.class, MemberCategory.values())
                .registerType(RedisTicketStorage.class, MemberCategory.values())
                .registerType(HazelcastTicketStorage.class, MemberCategory.values())
                .registerType(InMemoryTicketStorage.class, MemberCategory.values())
                .registerType(UnifiedAuthController.class, MemberCategory.values());
    }
}
