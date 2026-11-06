package dev.geng.bootstrap;

import dev.geng.application.entry.EntryAuthorization;
import dev.geng.application.entry.EntryAccessReader;
import dev.geng.application.transaction.ActorTransactions;
import dev.geng.domain.entry.EntryOperation;
import dev.geng.domain.error.DomainViolation;
import dev.geng.domain.error.FailureCode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/** Dedicated real PostgreSQL DB only. Never run with a production URL. */
@ActiveProfiles("local")
@SpringBootTest(properties = {"spring.datasource.hikari.maximum-pool-size=1", "spring.datasource.hikari.minimum-idle=1"})
class FoundationPostgresIT {
    @Autowired ActorTransactions transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntryAuthorization authorization;
    @Autowired EntryAccessReader entries;
    private Connection administrator;
    private final UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
    private final UUID ab = UUID.randomUUID(), ac = UUID.randomUUID();
    private final UUID entryAb = UUID.randomUUID(), entryAc = UUID.randomUUID();

    @BeforeEach void fixtures() throws Exception {
        String url = System.getenv("GENG_DB_URL");
        assertNotNull(url, "Provide the dedicated integration DB URL");
        assertTrue(url.endsWith("/geng_foundation_it"), "Integration fixtures must use the dedicated database");
        administrator = DriverManager.getConnection(url, "geng_admin", System.getenv("GENG_IT_ADMIN_PASSWORD"));
        for (var principal : List.of(a, b, c)) { admin("INSERT INTO identity.principals(id,status) VALUES (?, 'ACTIVE')", principal); }
        for (var space : List.of(ab, ac)) { admin("INSERT INTO space.spaces(id,status,version) VALUES (?, 'ACTIVE', 1)", space); }
        admin("INSERT INTO space.members(space_id,principal_id,slot) VALUES (?, ?, 1), (?, ?, 2), (?, ?, 1), (?, ?, 2)", ab,a,ab,b,ac,a,ac,c);
        // Opaque fixture bytes only; this does not claim to exercise content encryption.
        for (var pair : List.of(List.of(entryAb, ab), List.of(entryAc, ac))) {
            admin("INSERT INTO memory.entries(id,space_id,author_id,payload_ciphertext,wrapped_key,key_version,edit_mode,version) VALUES (?, ?, ?, decode('00','hex'), decode('00','hex'), 'test-only', 'AUTHOR_ONLY', 1)", pair.get(0), pair.get(1), a);
        }
    }

    private void admin(String sql, Object... values) throws SQLException {
        try (var statement = administrator.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) { statement.setObject(i + 1, values[i]); }
            statement.execute();
        }
    }

    @AfterEach void cleanup() throws Exception {
        if (administrator == null) return;
        try {
            admin("DROP TABLE IF EXISTS memory.transaction_probe");
            admin("DELETE FROM memory.entries WHERE space_id IN (?,?)", ab, ac);
            admin("DELETE FROM space.members WHERE space_id IN (?,?)", ab, ac);
            admin("DELETE FROM space.spaces WHERE id IN (?,?)", ab, ac);
            admin("DELETE FROM identity.principals WHERE id IN (?,?,?)", a,b,c);
        } finally { administrator.close(); }
    }

    @Test void runtimeRoleCannotAdministerOrReadOperations() {
        assertEquals("geng_app", jdbc.queryForObject("SELECT current_user", String.class));
        assertEquals(false, jdbc.queryForObject("SELECT rolsuper OR rolbypassrls OR rolcreatedb OR rolcreaterole FROM pg_roles WHERE rolname=current_user", Boolean.class));
        assertEquals(false, jdbc.queryForObject("SELECT pg_has_role(current_user,'geng_migrator','MEMBER')", Boolean.class));
        assertEquals(false, jdbc.queryForObject("SELECT has_schema_privilege(current_user,'ops','USAGE')", Boolean.class));
        assertEquals(false, jdbc.queryForObject("SELECT has_schema_privilege(current_user,'public','CREATE')", Boolean.class));
    }

    @Test void realRlsSeparatesThreeUsersAndFailsClosedWithoutIdentity() throws Exception {
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM memory.entries", Integer.class));
        assertEquals(2, transactions.read(a, () -> jdbc.queryForObject("SELECT count(*) FROM memory.entries", Integer.class)));
        assertEquals(List.of(entryAb), transactions.read(b, () -> jdbc.queryForList("SELECT id FROM memory.entries", UUID.class)));
        assertEquals(List.of(entryAc), transactions.read(c, () -> jdbc.queryForList("SELECT id FROM memory.entries", UUID.class)));
        admin("UPDATE identity.principals SET status='DELETING' WHERE id=?", b);
        assertEquals(0, transactions.read(b, () -> jdbc.queryForObject("SELECT count(*) FROM memory.entries", Integer.class)));
    }

    @Test void pooledActorIsClearedAfterBothCommitAndRollback() {
        Integer connection = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
        assertEquals(b.toString(), transactions.read(b, () -> jdbc.queryForObject("SELECT identity.current_actor()::text", String.class)));
        assertThrows(IllegalStateException.class, () -> transactions.write(a, () -> { throw new IllegalStateException("test rollback"); }));
        assertNull(jdbc.queryForObject("SELECT identity.current_actor()", UUID.class));
        assertEquals(connection, jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
        assertEquals(List.of(entryAc), transactions.read(c, () -> jdbc.queryForList("SELECT id FROM memory.entries", UUID.class)));
    }

    @Test void readWriteAndRollbackBoundariesAreEnforcedByPostgres() throws Exception {
        admin("CREATE TABLE memory.transaction_probe(id integer PRIMARY KEY)");
        admin("GRANT SELECT,INSERT ON memory.transaction_probe TO geng_app");
        assertThrows(org.springframework.dao.DataAccessException.class, () -> transactions.read(a, () -> jdbc.update("INSERT INTO memory.transaction_probe VALUES (1)")));
        assertThrows(IllegalStateException.class, () -> transactions.write(a, () -> {
            jdbc.update("INSERT INTO memory.transaction_probe VALUES (2)");
            throw new IllegalStateException("test rollback");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM memory.transaction_probe", Integer.class));
        assertEquals(1, transactions.write(a, () -> jdbc.update("INSERT INTO memory.transaction_probe VALUES (3)")));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM memory.transaction_probe", Integer.class));
        assertThrows(IllegalStateException.class, () -> transactions.read(a, () -> transactions.write(b, () -> 1)));
        assertNull(jdbc.queryForObject("SELECT identity.current_actor()", UUID.class));
    }

    @Test void readerAndPolicyUseSameTransactionAndAuthoritativeSpace() throws Exception {
        assertThrows(IllegalStateException.class, () -> entries.find(ab, entryAb));
        assertEquals(entryAb, transactions.read(b, () -> authorization.require(b, ab, entryAb, EntryOperation.READ)).entryId());
        assertEquals(FailureCode.NOT_FOUND, assertThrows(DomainViolation.class, () -> transactions.read(b, () -> authorization.require(b, ac, entryAc, EntryOperation.READ))).code());
        assertEquals(FailureCode.NOT_FOUND, assertThrows(DomainViolation.class, () -> transactions.read(a, () -> authorization.require(a, ac, entryAb, EntryOperation.READ))).code());
        assertEquals(FailureCode.ENTRY_EDIT_FORBIDDEN, assertThrows(DomainViolation.class, () -> transactions.write(b, () -> authorization.require(b, ab, entryAb, EntryOperation.EDIT))).code());
        admin("UPDATE space.spaces SET status='FROZEN', frozen_at=now() WHERE id=?", ab);
        assertEquals(entryAb, transactions.read(b, () -> authorization.require(b, ab, entryAb, EntryOperation.READ)).entryId());
        assertEquals(FailureCode.SPACE_FROZEN, assertThrows(DomainViolation.class, () -> transactions.write(a, () -> authorization.require(a, ab, entryAb, EntryOperation.EDIT))).code());
    }

    @Test void noGenericWriteGrantAndTwoPersonCapacityIsStructural() {
        assertThrows(org.springframework.dao.DataAccessException.class, () -> transactions.write(a, () -> jdbc.update("UPDATE memory.entries SET version=version+1")));
        assertEquals("23514", assertThrows(SQLException.class, () -> admin("INSERT INTO space.members(space_id,principal_id,slot) VALUES (?,?,3)", ab,c)).getSQLState());
        assertEquals("23505", assertThrows(SQLException.class, () -> admin("INSERT INTO space.members(space_id,principal_id,slot) VALUES (?,?,2)", ab,c)).getSQLState());
    }
}
