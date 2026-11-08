-- First executable subset of the reviewed logical model. No plaintext memories.
REVOKE ALL ON SCHEMA identity, space, memory, media, ops FROM PUBLIC;
GRANT USAGE ON SCHEMA identity, space, memory, media TO geng_app;
ALTER DEFAULT PRIVILEGES REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;

CREATE TABLE identity.principals (
    id uuid PRIMARY KEY,
    status varchar(32) NOT NULL CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'DELETING', 'DELETED')),
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE space.spaces (
    id uuid PRIMARY KEY,
    status varchar(16) NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'FROZEN')),
    version bigint NOT NULL CHECK (version > 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    frozen_at timestamptz,
    frozen_by uuid REFERENCES identity.principals(id),
    CHECK ((status = 'FROZEN') = (frozen_at IS NOT NULL))
);
CREATE TABLE space.members (
    space_id uuid NOT NULL REFERENCES space.spaces(id),
    principal_id uuid NOT NULL REFERENCES identity.principals(id),
    slot smallint NOT NULL CHECK (slot IN (1, 2)),
    joined_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (space_id, principal_id),
    UNIQUE (space_id, slot)
);
CREATE INDEX members_principal ON space.members(principal_id);
CREATE TABLE memory.entries (
    id uuid PRIMARY KEY,
    space_id uuid NOT NULL REFERENCES space.spaces(id),
    author_id uuid NOT NULL REFERENCES identity.principals(id),
    payload_ciphertext bytea NOT NULL,
    wrapped_key bytea NOT NULL,
    key_version varchar NOT NULL,
    edit_mode varchar(16) NOT NULL CHECK (edit_mode IN ('AUTHOR_ONLY', 'BOTH')),
    version bigint NOT NULL CHECK (version > 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (space_id, id),
    FOREIGN KEY (space_id, author_id) REFERENCES space.members(space_id, principal_id)
);
CREATE INDEX entries_space_order ON memory.entries(space_id, created_at, id);
CREATE INDEX entries_author ON memory.entries(author_id);

CREATE FUNCTION identity.current_actor() RETURNS uuid
LANGUAGE sql STABLE SET search_path = pg_catalog
AS $$ SELECT nullif(current_setting('geng.actor_id', true), '')::uuid $$;

-- Owner bypass is deliberately limited to this read-only membership predicate.
-- geng_app is neither an owner nor a member of geng_migrator.
CREATE FUNCTION space.can_read(target_space uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog
AS $$
    SELECT EXISTS (
        SELECT 1 FROM space.members m JOIN identity.principals p ON p.id = m.principal_id
        WHERE m.space_id = target_space AND p.id = identity.current_actor() AND p.status = 'ACTIVE'
    )
$$;
REVOKE ALL ON FUNCTION identity.current_actor(), space.can_read(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.current_actor(), space.can_read(uuid) TO geng_app;

ALTER TABLE identity.principals ENABLE ROW LEVEL SECURITY;
ALTER TABLE space.spaces ENABLE ROW LEVEL SECURITY;
ALTER TABLE space.members ENABLE ROW LEVEL SECURITY;
ALTER TABLE memory.entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE memory.entries FORCE ROW LEVEL SECURITY;
CREATE POLICY principal_self_read ON identity.principals FOR SELECT TO geng_app
    USING (id = identity.current_actor() AND status = 'ACTIVE');
CREATE POLICY spaces_member_read ON space.spaces FOR SELECT TO geng_app USING (space.can_read(id));
CREATE POLICY members_space_read ON space.members FOR SELECT TO geng_app USING (space.can_read(space_id));
CREATE POLICY entries_member_read ON memory.entries FOR SELECT TO geng_app USING (space.can_read(space_id));
GRANT SELECT ON identity.principals, space.spaces, space.members, memory.entries TO geng_app;
-- No INSERT/UPDATE/DELETE/TRUNCATE or generic DML policy until feature-specific
-- authorization, locks, crypto and permanent deletion have been implemented.
