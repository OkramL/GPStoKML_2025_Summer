package models;

import java.util.Objects;

/**
 * Represents a description entry associated with a specific date.
 * Includes both the current and a new (updated) description.
 * <p>
 * Used primarily to manage updates or overrides to descriptions based on date context.
 */
public class Description {
    private final String date;
    private final String currentDescription;
    private final String newDescription;

    /**
     * Constructs a new {@code Description} instance with the given date, current description,
     * and proposed new description.
     *
     * @param date               the date associated with the description (format: YYYY-MM-DD)
     * @param currentDescription the existing description string
     * @param newDescription     the updated or target description string
     */
    public Description(String date, String currentDescription, String newDescription) {
        this.date = date;
        this.currentDescription = currentDescription;
        this.newDescription = newDescription;
    }

    public String getDate() {
        return date;
    }

    public String getNewDescription() {
        return newDescription;
    }

    /**
     * Compares this {@code Description} object with another for equality.
     * Two descriptions are considered equal if their date and currentDescription fields match.
     *
     * @param o the object to compare to
     * @return {@code true} if the date and current description are equal; {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Description that = (Description) o;
        // return Objects.equals(date, that.date) && Objects.equals(currentDescription, that.currentDescription) && Objects.equals(newDescription, that.newDescription);
        return date.equals(that.date) && Objects.equals(currentDescription, that.currentDescription);
    }

    /**
     * Computes a hash code for this {@code Description} object using
     * the date, currentDescription, and newDescription fields.
     *
     * @return the computed hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(date, currentDescription, newDescription);
    }
}
