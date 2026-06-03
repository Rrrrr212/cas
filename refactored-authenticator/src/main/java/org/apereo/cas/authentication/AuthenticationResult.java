package org.apereo.cas.authentication;

import lombok.Builder;
import lombok.Data;
import org.apereo.cas.authentication.principal.Principal;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Result of an authentication process.
 */
@Data
@Builder
public class AuthenticationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Principal principal;

    private String authenticatorName;

    private boolean success;

    private LocalDateTime timestamp;

    public static class AuthenticationResultBuilder {
        private LocalDateTime timestamp = LocalDateTime.now();
    }
}
