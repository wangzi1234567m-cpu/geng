package dev.geng.infrastructure.entry;

import dev.geng.application.entry.EntryAccessReader;
import dev.geng.domain.entry.EditMode;
import dev.geng.domain.entry.EntryAccessSnapshot;
import dev.geng.domain.space.SpaceState;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Metadata only. Content decryption is deliberately outside this adapter. */
public final class JdbcEntryAccessReader implements EntryAccessReader {
    private final JdbcTemplate jdbc;
    public JdbcEntryAccessReader(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public Optional<EntryAccessSnapshot> find(UUID spaceId, UUID entryId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Entry access requires an actor transaction");
        }
        return jdbc.query("""
                SELECT e.space_id, e.id, e.author_id, s.status, e.edit_mode, e.version,
                       ARRAY(SELECT m.principal_id FROM space.members m WHERE m.space_id = e.space_id) AS readers
                FROM memory.entries e JOIN space.spaces s ON s.id = e.space_id
                WHERE e.space_id = ? AND e.id = ?
                """, (row, number) -> new EntryAccessSnapshot(
                        row.getObject("space_id", UUID.class), row.getObject("id", UUID.class),
                        row.getObject("author_id", UUID.class),
                        Set.copyOf(Arrays.asList((UUID[]) row.getArray("readers").getArray())),
                        SpaceState.valueOf(row.getString("status")), EditMode.valueOf(row.getString("edit_mode")),
                        row.getLong("version")), spaceId, entryId).stream().findFirst();
    }
}
