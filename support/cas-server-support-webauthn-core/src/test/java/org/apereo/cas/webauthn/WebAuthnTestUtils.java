package org.apereo.cas.webauthn;

import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import com.yubico.core.RegistrationStorage;
import com.yubico.core.SessionManager;
import org.springframework.beans.factory.ObjectProvider;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import static org.mockito.Mockito.*;

public class WebAuthnTestUtils {

    public static WebAuthnCredential getWebAuthnCredential() {
        return new WebAuthnCredential("token");
    }

    public static WebAuthnAuthenticationHandler getWebAuthnAuthenticationHandler(RegistrationStorage repository) {
        return new WebAuthnAuthenticationHandler("webauthn", 
            PrincipalFactoryUtils.newPrincipalFactory(), 
            repository, 
            mock(SessionManager.class), 
            0, 
            mock(ObjectProvider.class));
    }
}
