package dev.geng.domain.entry;

import dev.geng.domain.space.SpaceState;

import dev.geng.domain.error.DomainViolation;
import dev.geng.domain.error.FailureCode;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class EntryAccessPolicyTest {
    private final UUID author = UUID.randomUUID();
    private final UUID friend = UUID.randomUUID();
    private final EntryAccessPolicy policy = new EntryAccessPolicy();

    private EntryAccessSnapshot snapshot(SpaceState state, EditMode mode) {
        return new EntryAccessSnapshot(UUID.randomUUID(), UUID.randomUUID(), author,
                Set.of(author, friend), state, mode, 4);
    }

    @ParameterizedTest
    @EnumSource(EntryOperation.class)
    void outsiderCannotDiscoverEvenSharedOrFrozenEntry(EntryOperation operation) {
        var failure = assertThrows(DomainViolation.class, () -> policy.requirePermission(
                UUID.randomUUID(), snapshot(SpaceState.FROZEN, EditMode.BOTH), operation));
        assertEquals(FailureCode.NOT_FOUND, failure.code());
    }

    @ParameterizedTest
    @EnumSource(value = EntryOperation.class, names = {"EDIT", "DELETE", "CHANGE_PERMISSIONS"})
    void freezingStopsEveryOrdinaryMutationIncludingAuthorActions(EntryOperation operation) {
        var failure = assertThrows(DomainViolation.class, () -> policy.requirePermission(
                author, snapshot(SpaceState.FROZEN, EditMode.BOTH), operation));
        assertEquals(FailureCode.SPACE_FROZEN, failure.code());
    }

    @Test
    void formerMembersCanStillReadFrozenHistory() {
        assertDoesNotThrow(() -> policy.requirePermission(friend,
                snapshot(SpaceState.FROZEN, EditMode.AUTHOR_ONLY), EntryOperation.READ));
    }

    @ParameterizedTest
    @EnumSource(value = EntryOperation.class, names = {"EDIT", "DELETE"})
    void sharingAllowsFriendEditingAndDeletionButDefaultModeDoesNot(EntryOperation operation) {
        assertDoesNotThrow(() -> policy.requirePermission(friend,
                snapshot(SpaceState.ACTIVE, EditMode.BOTH), operation));
        var failure = assertThrows(DomainViolation.class, () -> policy.requirePermission(friend,
                snapshot(SpaceState.ACTIVE, EditMode.AUTHOR_ONLY), operation));
        assertEquals(FailureCode.ENTRY_EDIT_FORBIDDEN, failure.code());
    }

    @Test
    void jointEditingNeverGrantsFriendControlOfEditMode() {
        var entry = snapshot(SpaceState.ACTIVE, EditMode.BOTH);
        assertEquals(FailureCode.ENTRY_PERMISSIONS_FORBIDDEN,
                assertThrows(DomainViolation.class, () -> policy.requirePermission(friend,
                        entry, EntryOperation.CHANGE_PERMISSIONS)).code());
        assertDoesNotThrow(() -> policy.requirePermission(author, entry, EntryOperation.CHANGE_PERMISSIONS));
    }

    @Test
    void staleVersionCannotBeAcceptedAsCurrent() {
        var entry = snapshot(SpaceState.ACTIVE, EditMode.BOTH);
        assertDoesNotThrow(() -> policy.requireVersion(entry, 4));
        assertEquals(FailureCode.ENTRY_VERSION_CONFLICT,
                assertThrows(DomainViolation.class, () -> policy.requireVersion(entry, 3)).code());
    }
}
