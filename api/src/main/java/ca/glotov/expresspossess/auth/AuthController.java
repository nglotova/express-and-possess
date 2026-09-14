package ca.glotov.expresspossess.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Everything a visitor can do before they are logged in: register, log in, ask for a
 * password reset and use the link. Logout is handled by Spring Security at
 * {@code POST /api/auth/logout}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accounts;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    AuthController(AccountService accounts,
                   AuthenticationManager authenticationManager,
                   SecurityContextRepository securityContextRepository) {
        this.accounts = accounts;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    record RegisterRequest(@NotBlank @Email @Size(max = 254) String email,
                           @NotBlank @Size(min = 8, max = 72) String password,
                           @NotBlank @Size(max = 100) String name) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    record PasswordResetRequest(@NotBlank @Email String email) {
    }

    record PasswordResetConfirm(@NotBlank String token, @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    /**
     * Does nothing except set the CSRF cookie. The web app calls it before its first write
     * of a visit, which may be the registration itself.
     */
    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void csrf() {
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest body,
                          HttpServletRequest request, HttpServletResponse response) {
        User user = accounts.register(body.email(), body.password(), body.name());
        logIn(body.email(), body.password(), request, response);
        return UserResponse.of(user);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest body,
                       HttpServletRequest request, HttpServletResponse response) {
        AuthenticatedUser principal = logIn(body.email(), body.password(), request, response);
        return UserResponse.of(accounts.get(principal.getId()));
    }

    @PostMapping("/password-reset/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestPasswordReset(@Valid @RequestBody PasswordResetRequest body) {
        accounts.requestPasswordReset(body.email());
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirm body) {
        accounts.confirmPasswordReset(body.token(), body.newPassword());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    void badCredentials() {
    }

    private AuthenticatedUser logIn(String email, String password,
                                    HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Wrong email or password");
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
