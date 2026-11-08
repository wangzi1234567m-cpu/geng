package dev.geng.application.entry;

import dev.geng.domain.space.SpaceState;

import dev.geng.domain.entry.*;
import dev.geng.domain.error.DomainViolation;
import dev.geng.domain.error.FailureCode;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EntryAuthorizationTest {
    @Test
    void adapterReturningAnotherSpacesEntryCannotAuthorizeIt() {
        UUID actor = UUID.randomUUID();
        UUID requestedSpace = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        var other = new EntryAccessSnapshot(UUID.randomUUID(), entryId, actor,
                Set.of(actor), SpaceState.ACTIVE, EditMode.BOTH, 1);
        var useCase = new EntryAuthorization((space, entry) -> Optional.of(other), new EntryAccessPolicy());
        assertEquals(FailureCode.NOT_FOUND, assertThrows(DomainViolation.class,
                () -> useCase.require(actor, requestedSpace, entryId, EntryOperation.EDIT)).code());
    }

    @Test
    void aMissingRecordAndARecordOutsideMembershipHaveTheSameFailure() {
        UUID actor = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        UUID space = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        var existing = new EntryAccessSnapshot(space, entryId, author,
                Set.of(author), SpaceState.ACTIVE, EditMode.BOTH, 1);
        var absent = new EntryAuthorization((s, e) -> Optional.empty(), new EntryAccessPolicy());
        var hidden = new EntryAuthorization((s, e) -> Optional.of(existing), new EntryAccessPolicy());
        assertEquals(FailureCode.NOT_FOUND, assertThrows(DomainViolation.class,
                () -> absent.require(actor, space, entryId, EntryOperation.READ)).code());
        assertEquals(FailureCode.NOT_FOUND, assertThrows(DomainViolation.class,
                () -> hidden.require(actor, space, entryId, EntryOperation.READ)).code());
    }
}
