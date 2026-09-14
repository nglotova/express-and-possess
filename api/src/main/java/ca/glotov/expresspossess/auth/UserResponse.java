package ca.glotov.expresspossess.auth;

public record UserResponse(Long id, String email, String name, Role role, boolean emailEnabled) {

    public static UserResponse of(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.isEmailEnabled());
    }
}
