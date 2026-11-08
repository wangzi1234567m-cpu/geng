package dev.geng.application.transaction;

import java.util.UUID;
import java.util.function.Supplier;

/** Actor must come from verified authentication, never a client-supplied actor field. */
public interface ActorTransactions {
    <T> T read(UUID actor, Supplier<T> work);
    <T> T write(UUID actor, Supplier<T> work);
}
