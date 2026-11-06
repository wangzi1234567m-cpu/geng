package dev.geng.domain.entry;

/** Ordinary user actions only. Account erasure is a separate controlled use case. */
public enum EntryOperation { READ, EDIT, DELETE, CHANGE_PERMISSIONS }
