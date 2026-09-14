package ca.glotov.expresspossess.groups;

/**
 * What is stored for a group. "New" and "Working" from the spec are computed from whether
 * the group has any expressions and are reported through {@link GroupSummary.Status}.
 */
public enum GroupStatus {
    ACTIVE,
    CLOSED,
    ARCHIVED
}
