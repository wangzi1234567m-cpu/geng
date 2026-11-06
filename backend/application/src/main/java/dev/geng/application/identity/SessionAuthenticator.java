package dev.geng.application.identity;

import java.util.Optional;
import java.util.UUID;

/** Future opaque-session adapter. No adapter/HTTP login is enabled in phase 1. */
public interface SessionAuthenticator {
    /** Empty for expired/revoked sessions or accounts without verified active access. Never log the token. */
    Optional<UUID> authenticate(String accessToken);
}
