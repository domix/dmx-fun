package dmx.fun.tutorial.domain;

/** A course with a limited number of seats and a fee in cents. */
public record Course(String id, String title, int capacity, int feeCents) {}
