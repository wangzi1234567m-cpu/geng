package dev.geng.domain.entry;

import dev.geng.domain.space.SpaceState;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record EntryAccessSnapshot(
        UUID spaceId, UUID entryId, UUID authorId, Set<UUID> readers,
        SpaceState spaceState, EditMode editMode, long version) {
    public EntryAccessSnapshot {
        Objects.requireNonNull(spaceId);
        Objects.requireNonNull(entryId);
        Objects.requireNonNull(authorId);
        readers = Set.copyOf(readers);
        Objects.requireNonNull(spaceState);
        Objects.requireNonNull(editMode);
        if (version < 1) {
            throw new IllegalArgumentException("Version must be positive");
        }
    }
}
