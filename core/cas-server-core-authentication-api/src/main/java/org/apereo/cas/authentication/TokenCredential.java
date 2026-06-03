package org.apereo.cas.authentication;

import module java.base;
import org.apereo.cas.authentication.credential.BasicIdentifiableCredential;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenCredential extends BasicIdentifiableCredential {

    private final String protocol;

    public TokenCredential(final String token, final String protocol) {
        super(token);
        this.protocol = protocol;
    }
}