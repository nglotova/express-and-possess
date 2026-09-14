package ca.glotov.expresspossess.demo;

import ca.glotov.expresspossess.auth.AuthController;
import ca.glotov.expresspossess.auth.UserResponse;
import ca.glotov.expresspossess.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The "Log in as Alice / Bob / Carol" buttons. Both endpoints answer 404 unless the
 * instance runs in demo mode, so the family instance does not even reveal they exist.
 */
@RestController
@RequestMapping("/api/auth/demo")
class DemoController {

    private final DemoSeeder demo;
    private final DemoProperties properties;
    private final AuthController auth;

    DemoController(DemoSeeder demo, DemoProperties properties, AuthController auth) {
        this.demo = demo;
        this.properties = properties;
        this.auth = auth;
    }

    record DemoInfo(boolean enabled, List<String> personas) {
    }

    @GetMapping
    DemoInfo info() {
        return new DemoInfo(demo.enabled(), demo.enabled() ? DemoSeeder.PERSONAS : List.of());
    }

    @PostMapping("/{persona}")
    UserResponse loginAs(@PathVariable String persona, HttpServletRequest request, HttpServletResponse response) {
        if (!demo.enabled() || !DemoSeeder.PERSONAS.contains(persona)) {
            throw ApiException.notFound("Not found");
        }
        return auth.login(new AuthController.LoginRequest(DemoSeeder.emailOf(persona), properties.password()),
                request, response);
    }
}
