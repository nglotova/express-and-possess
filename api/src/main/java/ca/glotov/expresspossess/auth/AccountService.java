package ca.glotov.expresspossess.auth;

import ca.glotov.expresspossess.common.ApiException;
import ca.glotov.expresspossess.common.AppProperties;
import ca.glotov.expresspossess.common.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Transactional
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final EmailService email;
    private final AppProperties properties;
    private final Clock clock;

    AccountService(UserRepository users,
                   PasswordResetTokenRepository resetTokens,
                   PasswordEncoder passwordEncoder,
                   EmailService email,
                   AppProperties properties,
                   Clock clock) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.properties = properties;
        this.clock = clock;
    }

    public User register(String emailAddress, String password, String name) {
        if (users.existsByEmailIgnoreCase(emailAddress)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        return users.save(new User(emailAddress.trim(), passwordEncoder.encode(password), name.trim()));
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("No such user"));
    }

    public User updateProfile(Long id, String name, boolean emailEnabled) {
        User user = get(id);
        user.setName(name.trim());
        user.setEmailEnabled(emailEnabled);
        return user;
    }

    public void changePassword(Long id, String currentPassword, String newPassword) {
        User user = get(id);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is wrong");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    /**
     * Sends a reset link if the address has an account. Silently does nothing otherwise, so
     * the endpoint cannot be used to find out which emails are registered.
     */
    public void requestPasswordReset(String emailAddress) {
        users.findByEmailIgnoreCase(emailAddress).ifPresent(user -> {
            String token = newToken();
            Instant expires = clock.instant().plus(properties.passwordResetTtl());
            resetTokens.save(new PasswordResetToken(user, hash(token), expires));
            String link = properties.baseUrl() + "/reset-password?token=" + token;
            email.send(user.getEmail(), "Reset your Express & Possess password",
                    "Hello " + user.getName() + ",\n\n"
                            + "Open this link to choose a new password:\n" + link + "\n\n"
                            + "The link works once and expires in "
                            + properties.passwordResetTtl().toMinutes() + " minutes. "
                            + "If you did not ask for it, ignore this email.\n");
        });
    }

    public void confirmPasswordReset(String token, String newPassword) {
        Instant now = clock.instant();
        PasswordResetToken reset = resetTokens.findByTokenHash(hash(token))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> ApiException.badRequest("This reset link is invalid or has expired"));
        reset.getUser().setPasswordHash(passwordEncoder.encode(newPassword));
        reset.markUsed(now);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
