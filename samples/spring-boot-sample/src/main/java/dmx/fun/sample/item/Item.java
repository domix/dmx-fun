package dmx.fun.sample.item;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain record for a catalog item.
 *
 * <p>Spring Data JDBC maps each component to a column of the same name.
 * A {@code null} id signals a new entity; Spring Data JDBC sets it after INSERT.
 */
@Table("items")
public record Item(@Id Long id, String name, String description) {}
