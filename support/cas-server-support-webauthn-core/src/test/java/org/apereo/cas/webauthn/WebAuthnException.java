package org.apereo.cas.webauthn;

import org.apereo.cas.authentication.RootCasException;

public class WebAuthnException extends RootCasException {

    private static final long serialVersionUID = 1L;

    public WebAuthnException(final String code, final String message) {
        super(code, message);
    }

    public WebAuthnException(final String code) {
        super(code);
    }
}
