package ca.glotov.expresspossess.common;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Web addresses inside free text. The web app finds links with the same rules in
 * {@code web/src/components/links.tsx}; keep the two in step.
 */
public final class Links {

    private static final Pattern WEB_ADDRESS = Pattern.compile("https?://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);

    /** Punctuation that ends a sentence rather than the address: "like this: https://x.ca/a." */
    private static final String TRAILING = ".,;:!?)]}";

    private Links() {
    }

    /** Every address in the text, in order, each once. */
    public static List<String> all(String text) {
        if (text == null) {
            return List.of();
        }
        LinkedHashSet<String> found = new LinkedHashSet<>();
        Matcher matcher = WEB_ADDRESS.matcher(text);
        while (matcher.find()) {
            String url = trimTrailing(matcher.group());
            if (url.indexOf("://") + 3 < url.length()) {
                found.add(url);
            }
        }
        return List.copyOf(found);
    }

    /** The first address in the text, or null. */
    public static String first(String text) {
        List<String> all = all(text);
        return all.isEmpty() ? null : all.get(0);
    }

    /** The text with its addresses taken out and the empty lines that leaves dropped. */
    public static String withoutLinks(String text) {
        if (text == null) {
            return "";
        }
        return WEB_ADDRESS.matcher(text).replaceAll("").lines()
                .map(line -> line.replaceAll("\\s+", " ").strip())
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n"));
    }

    /** The site a link points to, without "www.": "amazon.ca". The link itself when it has no host. */
    public static String site(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? url : host.replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

        private static String trimTrailing(String url) {
        int end = url.length();
        while (end > 0 && TRAILING.indexOf(url.charAt(end - 1)) >= 0) {
            end--;
        }
        return url.substring(0, end);
    }
}
