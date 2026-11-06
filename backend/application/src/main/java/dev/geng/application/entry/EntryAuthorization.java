package dev.geng.application.entry;

import dev.geng.domain.entry.EntryAccessPolicy;
import dev.geng.domain.entry.EntryAccessSnapshot;
import dev.geng.domain.entry.EntryOperation;
import dev.geng.domain.error.DomainViolation;
import dev.geng.domain.error.FailureCode;
import java.util.Objects;
import java.util.UUID;

/** Authorization component, not a persistence command or transaction implementation. */
public final class EntryAuthorization {
    private final EntryAccessReader reader;
    private final EntryAccessPolicy policy;

    public EntryAuthorization(EntryAccessReader reader, EntryAccessPolicy policy) {
        this.reader = Objects.requireNonNull(reader);
        this.policy = Objects.requireNonNull(policy);
    }

    public EntryAccessSnapshot require(UUID actor, UUID spaceId, UUID entryId, EntryOperation operation) {
        var entry = reader.find(spaceId, entryId)
                .filter(value -> value.spaceId().equals(spaceId) && value.entryId().equals(entryId))
                .orElseThrow(() -> new DomainViolation(FailureCode.NOT_FOUND));
        policy.requirePermission(actor, entry, operation);
        return entry;
    }
}
