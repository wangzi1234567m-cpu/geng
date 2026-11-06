package dev.geng.infrastructure.transaction;

import dev.geng.application.transaction.ActorTransactions;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class JdbcActorTransactions implements ActorTransactions {
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager manager;

    public JdbcActorTransactions(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.manager = manager;
    }

    @Override public <T> T read(UUID actor, Supplier<T> work) { return execute(actor, work, true); }
    @Override public <T> T write(UUID actor, Supplier<T> work) { return execute(actor, work, false); }

    private <T> T execute(UUID actor, Supplier<T> work, boolean readOnly) {
        Objects.requireNonNull(actor);
        Objects.requireNonNull(work);
        // One application use case owns one boundary: disallow nested identity changes
        // and accidental upgrades from a read transaction to a write transaction.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Actor transaction must be the outer boundary");
        }
        var transaction = new TransactionTemplate(manager);
        transaction.setReadOnly(readOnly);
        return transaction.execute(status -> {
            // true = transaction-local; commit AND rollback clear the pooled identity.
            jdbc.queryForObject("SELECT pg_catalog.set_config('geng.actor_id', ?, true)", String.class, actor.toString());
            return work.get();
        });
    }
}
