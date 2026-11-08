package dev.geng.kaogengji.core

import org.junit.Assert.*
import org.junit.Test

class BackendOriginTest {
    @Test fun releaseRejectsCleartextEvenForLocalhost() {
        assertThrows(IllegalArgumentException::class.java) { BackendOrigin.parse("http://127.0.0.1:8080", false) }
    }
    @Test fun debugAllowsOnlyLocalCleartextTargets() {
        assertEquals("/actuator/health", BackendOrigin.parse("http://10.0.2.2:8080", true).healthUri.path)
        assertThrows(IllegalArgumentException::class.java) { BackendOrigin.parse("http://example.com", true) }
    }
    @Test fun originsCannotCarryCredentialsOrQuerySecrets() {
        assertThrows(IllegalArgumentException::class.java) { BackendOrigin.parse("https://user:secret@example.com", false) }
        assertThrows(IllegalArgumentException::class.java) { BackendOrigin.parse("https://example.com?token=secret", false) }
        assertThrows(IllegalArgumentException::class.java) { BackendOrigin.parse("https://example.com/api", false) }
    }
}
