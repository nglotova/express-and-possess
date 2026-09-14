package ca.glotov.expresspossess.groups;

public record MemberView(Long userId, String name, String email, GroupMemberRole role, boolean hasExpressions) {

    /** Used by the JPQL projection; the mark is filled in afterwards. */
    public MemberView(Long userId, String name, String email, GroupMemberRole role) {
        this(userId, name, email, role, false);
    }

    MemberView withHasExpressions(boolean value) {
        return new MemberView(userId, name, email, role, value);
    }
}
