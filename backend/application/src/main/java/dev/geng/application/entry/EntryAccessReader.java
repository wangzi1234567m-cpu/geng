package dev.geng.application.entry;

import dev.geng.domain.entry.EntryAccessSnapshot;
import java.util.Optional;
import java.util.UUID;

/** Adapter must scope by both IDs and load authoritative state in the caller's transaction. */
public interface EntryAccessReader {
    Optional<EntryAccessSnapshot> find(UUID spaceId, UUID entryId);
}
