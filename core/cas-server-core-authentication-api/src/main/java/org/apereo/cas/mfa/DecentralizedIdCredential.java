package org.apereo.cas.mfa;

import module java.base;
import org.apereo.cas.authentication.credential.OneTimeTokenCredential;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.io.Serializable;
import java.util.Map;

/**
 * This is {@link DecentralizedIdCredential}.
 *
 * @author CAS
 * @since 7.2.0
 */
@ToString
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DecentralizedIdCredential extends OneTimeTokenCredential {
    @Serial
    private static final long serialVersionUID = -1234567890123456789L;

    private String decentralizedId;

    private String didMethod;

    private String credentialId;

    private Map<String, Object> didDocument;

    public DecentralizedIdCredential(final String token) {
        super(token);
    }

    public DecentralizedIdCredential(final String token, final String decentralizedId,
                                      final String didMethod, final String credentialId,
                                      final Map<String, Object> didDocument) {
        super(token);
        this.decentralizedId = decentralizedId;
        this.didMethod = didMethod;
        this.credentialId = credentialId;
        this.didDocument = didDocument;
    }
}
