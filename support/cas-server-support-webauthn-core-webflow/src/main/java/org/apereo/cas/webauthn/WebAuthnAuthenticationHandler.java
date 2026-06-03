package org.apereo.cas.webauthn;

import module java.base;
import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.MultifactorAuthenticationHandler;
import org.apereo.cas.authentication.MultifactorAuthenticationProvider;
import org.apereo.cas.authentication.handler.support.AbstractPreAndPostProcessingAuthenticationHandler;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.mfa.DecentralizedIdCredential;
import org.apereo.cas.monitor.Monitorable;
import org.apereo.cas.web.support.WebUtils;
import org.apereo.cas.webauthn.storage.WebAuthnCredentialRepository;
import com.yubico.core.SessionManager;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Getter
@Monitorable
public class WebAuthnAuthenticationHandler extends AbstractPreAndPostProcessingAuthenticationHandler implements MultifactorAuthenticationHandler {
    private static final String PARAMETER_DID_DOCUMENT = "didDocument";

    private static final String HEADER_DID_DOCUMENT = "X-CAS-DID-Document";

    private final WebAuthnCredentialRepository webAuthnCredentialRepository;

    private final SessionManager sessionManager;

    private final ObjectProvider<MultifactorAuthenticationProvider> multifactorAuthenticationProvider;

    public WebAuthnAuthenticationHandler(final String name,
                                         final PrincipalFactory principalFactory,
                                         final WebAuthnCredentialRepository webAuthnCredentialRepository,
                                         final SessionManager sessionManager,
                                         final Integer order,
                                         final ObjectProvider<MultifactorAuthenticationProvider> multifactorAuthenticationProvider) {
        super(name, principalFactory, order);
        this.webAuthnCredentialRepository = webAuthnCredentialRepository;
        this.sessionManager = sessionManager;
        this.multifactorAuthenticationProvider = multifactorAuthenticationProvider;
    }

    @Override
    public boolean supports(final Credential credential) {
        return WebAuthnCredential.class.isAssignableFrom(credential.getClass());
    }

    @Override
    public boolean supports(final Class<? extends Credential> clazz) {
        return WebAuthnCredential.class.isAssignableFrom(clazz);
    }

    @Override
    protected AuthenticationHandlerExecutionResult doAuthentication(final Credential credential, final Service service) throws Throwable {
        val webAuthnCredential = (WebAuthnCredential) credential;
        val authentication = Objects.requireNonNull(WebUtils.getInProgressAuthentication(),
            "CAS has no reference to an authentication event to locate a principal");
        val principal = authentication.getPrincipal();
        val uid = principal.getId();
        val credentials = new LinkedHashSet<>(webAuthnCredentialRepository.getCredentialIdsForUsername(uid));
        credentials.addAll(resolveCredentialIdsForDid(uid));
        if (credentials.isEmpty()) {
            throw new AccountNotFoundException("Unable to locate registration record for " + uid);
        }
        return createHandlerResult(webAuthnCredential, this.principalFactory.createPrincipal(uid));
    }

    private Set<PublicKeyCredentialDescriptor> resolveCredentialIdsForDid(final String uid) {
        return locateDidDocument()
            .map(didDocument -> {
                val credentialIds = DecentralizedIdCredential.resolveCredentialIds(didDocument);
                val candidateIdentifiers = DecentralizedIdCredential.parse(didDocument).stream()
                    .map(DecentralizedIdCredential::getDid)
                    .filter(Objects::nonNull)
                    .filter(Predicate.not(String::isBlank))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
                candidateIdentifiers.add(uid);
                return candidateIdentifiers.stream()
                    .map(webAuthnCredentialRepository::getCredentialIdsForUsername)
                    .flatMap(Collection::stream)
                    .filter(descriptor -> credentialIds.isEmpty() || credentialIds.contains(descriptor.getId().getBase64Url()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            })
            .orElseGet(Set::of);
    }

    private Optional<String> locateDidDocument() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
            .filter(ServletRequestAttributes.class::isInstance)
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest)
            .map(request -> {
                val parameter = request.getParameter(PARAMETER_DID_DOCUMENT);
                if (StringUtils.isNotBlank(parameter)) {
                    return parameter;
                }
                return request.getHeader(HEADER_DID_DOCUMENT);
            })
            .filter(StringUtils::isNotBlank);
    }
}
