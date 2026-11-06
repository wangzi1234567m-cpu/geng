package dev.geng.domain.entry;

import dev.geng.domain.space.SpaceState;

import dev.geng.domain.error.DomainViolation;
import dev.geng.domain.error.FailureCode;
import java.util.Objects;
import java.util.UUID;

/** Pure permission rules. Call inside the final write transaction; this does not lock data. */
public final class EntryAccessPolicy {
    public void requirePermission(UUID actor, EntryAccessSnapshot entry, EntryOperation operation) {
        Objects.requireNonNull(actor);
        Objects.requireNonNull(operation);
        if (!entry.readers().contains(actor)) {
            throw new DomainViolation(FailureCode.NOT_FOUND);
        }
        if (operation == EntryOperation.READ) {
            return;
        }
        if (entry.spaceState() == SpaceState.FROZEN) {
            throw new DomainViolation(FailureCode.SPACE_FROZEN);
        }
        // Pending-space publishing policy is undecided: fail closed, never invent access.
        if (entry.spaceState() != SpaceState.ACTIVE) {
            throw new DomainViolation(FailureCode.SPACE_NOT_ACTIVE);
        }
        if (operation == EntryOperation.CHANGE_PERMISSIONS) {
            if (!entry.authorId().equals(actor)) {
                throw new DomainViolation(FailureCode.ENTRY_PERMISSIONS_FORBIDDEN);
            }
            return;
        }
        if (!entry.authorId().equals(actor) && entry.editMode() != EditMode.BOTH) {
            throw new DomainViolation(FailureCode.ENTRY_EDIT_FORBIDDEN);
        }
    }

    public void requireVersion(EntryAccessSnapshot entry, long expectedVersion) {
        if (entry.version() != expectedVersion) {
            throw new DomainViolation(FailureCode.ENTRY_VERSION_CONFLICT);
        }
    }
}
