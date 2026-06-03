package org.apereo.cas.webauthn;

import module java.base;
import org.apereo.cas.authentication.AuthenticationHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

public abstract class AbstractCasMockitoTest {

    @BeforeEach
    void setUpMockito() {
        AuthenticationHolder.clear();
    }

    @AfterEach
    void tearDownMockito() {
        AuthenticationHolder.clear();
        Mockito.framework().clearInlineMocks();
    }
}