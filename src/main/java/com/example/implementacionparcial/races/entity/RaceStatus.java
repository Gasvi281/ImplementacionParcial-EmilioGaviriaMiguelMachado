package com.example.implementacionparcial.races.entity;

/**
 * Declared in one-way progression order (see {@link #ordinal()} usage in
 * {@code RaceService}): a race can only ever move to a status with a higher ordinal,
 * except for the terminal {@link #CANCELLED}, which is reachable from any non-terminal
 * status. {@link #COMPLETED} and {@link #CANCELLED} are terminal and accept no further
 * transitions.
 */
public enum RaceStatus {
    DRAFT,
    OPEN_FOR_REGISTRATION,
    CLOSED_FOR_REGISTRATION,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
