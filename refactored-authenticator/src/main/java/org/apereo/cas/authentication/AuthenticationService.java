package org.apereo.cas.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.Principal;
import org.springframework.stereotype.Service;

/**
 * Main authentication service that coordinates the authentication process
 * using Strategy pattern and Factory pattern.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticatorFactory authenticatorFactory;

    public AuthenticationResult authenticate(Credential credential) 
        throws AuthenticationException {
        
        LOGGER.info("Starting authentication for credential: [{}]", credential.getId());

        Authenticator<?, ?> authenticator = authenticatorFactory.getAuthenticator(credential)
            .orElseThrow(() -> new AuthenticationException(
                "No authenticator found for credential type: " + credential.getClass().getName()
            ));

        LOGGER.debug("Using authenticator: [{}]", authenticator.getName());

        @SuppressWarnings("unchecked")
        Authenticator<Credential, Principal> typedAuthenticator = 
            (Authenticator<Credential, Principal>) authenticator;

        Principal principal = typedAuthenticator.authenticate(credential);
        
        AuthenticationResult result = AuthenticationResult.builder()
            .principal(principal)
            .authenticatorName(authenticator.getName())
            .success(true)
            .build();

        LOGGER.info("Authentication successful for principal: [{}]", principal.getId());
        return result;
    }
}
