package ca.glotov.expresspossess.expressions;

import ca.glotov.expresspossess.common.ApiException;
import ca.glotov.expresspossess.common.AppProperties;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Pictures on disk under {@code app.uploads-dir}, served back through {@link FileController}.
 * Names are random, so a URL cannot be guessed, and the extension comes from the content
 * type rather than from the uploaded file name.
 */
@Service
public class FileStorage {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private final Path root;

    FileStorage(AppProperties properties) {
        this.root = Path.of(properties.uploadsDir()).toAbsolutePath().normalize();
    }

    /** Stores the file and returns the URL path the page can use. */
    public String store(MultipartFile file) {
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null || file.isEmpty()) {
            throw ApiException.badRequest("Only JPEG, PNG, WebP or GIF pictures are accepted");
        }
        String name = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(root);
            file.transferTo(root.resolve(name));
        } catch (IOException e) {
            throw new IllegalStateException("Could not store " + name, e);
        }
        return "/api/files/" + name;
    }

    /** Stores picture bytes fetched from a shop, under the same type rules as an upload. */
    public String store(byte[] bytes, String contentType) {
        String extension = EXTENSIONS.get(contentType);
        if (extension == null || bytes.length == 0) {
            throw ApiException.badRequest("Only JPEG, PNG, WebP or GIF pictures are accepted");
        }
        String name = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(root);
            Files.write(root.resolve(name), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store " + name, e);
        }
        return "/api/files/" + name;
    }

    Resource load(String name) {
        Path path = root.resolve(name).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw ApiException.notFound("No such file");
        }
        return new PathResource(path);
    }

    static String contentTypeOf(String name) {
        String extension = name.substring(name.lastIndexOf('.') + 1);
        return EXTENSIONS.entrySet().stream()
                .filter(e -> e.getValue().equals(extension))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("application/octet-stream");
    }
}
