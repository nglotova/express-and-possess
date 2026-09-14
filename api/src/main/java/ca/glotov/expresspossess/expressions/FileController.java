package ca.glotov.expresspossess.expressions;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Serves uploaded pictures to logged-in members. */
@RestController
class FileController {

    private final FileStorage files;

    FileController(FileStorage files) {
        this.files = files;
    }

    @GetMapping("/api/files/{name:[A-Za-z0-9-]+\\.[a-z]+}")
    ResponseEntity<Resource> get(@PathVariable String name) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(FileStorage.contentTypeOf(name)))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate())
                .body(files.load(name));
    }
}
