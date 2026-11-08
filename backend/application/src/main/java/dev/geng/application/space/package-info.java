/**
 * Invitation acceptance, space reads and freezing use cases.
 * Future writes lock the space before records and perform checks in the same
 * ActorTransactions boundary. Pending-space publishing remains undecided.
 */
package dev.geng.application.space;
