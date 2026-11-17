package dmx.fun.tutorial.chapter08;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** When a course meets. */
public record Schedule(DayOfWeek day, LocalTime start) {}
