package dev.geng.domain.identity;

/** DELETING and DELETED never qualify for normal space access. */
public enum AccountState { PENDING_VERIFICATION, ACTIVE, DELETING, DELETED }
