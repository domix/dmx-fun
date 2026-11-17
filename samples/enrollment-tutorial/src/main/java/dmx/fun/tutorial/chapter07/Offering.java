package dmx.fun.tutorial.chapter07;

import dmx.fun.NonEmptyList;
import dmx.fun.tutorial.domain.Course;

/**
 * A course as actually offered this term: the course plus when it meets.
 *
 * <p>A course with no schedule cannot be offered, so the type says so. {@code NonEmptyList} cannot
 * be constructed empty, which means {@code schedules().head()} always exists and nobody ever
 * writes {@code if (schedules.isEmpty())} for a situation that cannot happen.
 */
public record Offering(Course course, NonEmptyList<Schedule> schedules) {}
