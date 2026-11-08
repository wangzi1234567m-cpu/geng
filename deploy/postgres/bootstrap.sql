-- Run only against a fresh, project-owned development database, as administrator.
\set ON_ERROR_STOP on
CREATE ROLE geng_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD :'app_password';
CREATE ROLE geng_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD :'migration_password';
REVOKE ALL ON DATABASE geng_dev FROM PUBLIC;
GRANT CONNECT ON DATABASE geng_dev TO geng_app, geng_migrator;
GRANT CREATE ON DATABASE geng_dev TO geng_migrator;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA ops AUTHORIZATION geng_migrator;
-- The remaining schemas are created by Flyway V1, not a second implementation.
ALTER ROLE geng_app SET search_path TO pg_catalog;
ALTER ROLE geng_app SET statement_timeout TO '10s';
ALTER ROLE geng_migrator SET search_path TO pg_catalog;
