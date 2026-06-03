package org.apereo.cas.webauthn;

import module java.base;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class WebAuthnException extends AccountException {

    @Serial
    private static final long serialVersionUID = -4536789021245678901L;

    public WebAuthnException(final String msg) {
        super(msg);
    }
}