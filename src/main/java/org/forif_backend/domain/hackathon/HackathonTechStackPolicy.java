package org.forif_backend.domain.hackathon;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class HackathonTechStackPolicy {

    public static final int MAX_COUNT = 4;
    public static final int MAX_NAME_LENGTH = 50;

    private static final List<String> OPTIONS = List.of(
            "React",
            "Next.js",
            "TypeScript",
            "JavaScript",
            "HTML",
            "CSS",
            "Vite",
            "Spring Boot",
            "Node.js",
            "Python",
            "PostgreSQL",
            "Supabase",
            "OpenAI API",
            "ChatGPT",
            "Vercel",
            "GitHub"
    );

    private static final Map<String, String> CANONICAL_NAME_BY_NORMALIZED = OPTIONS.stream()
            .collect(Collectors.toUnmodifiableMap(
                    HackathonTechStackPolicy::normalize,
                    Function.identity()
            ));

    private HackathonTechStackPolicy() {
    }

    public static String normalize(String techStack) {
        if (techStack == null) {
            return "";
        }
        return techStack.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static String canonicalize(String techStack) {
        String displayName = techStack == null ? "" : techStack.trim().replaceAll("\\s+", " ");
        return CANONICAL_NAME_BY_NORMALIZED.getOrDefault(normalize(displayName), displayName);
    }

    public static boolean isValid(String techStack) {
        return !techStack.isBlank() && techStack.length() <= MAX_NAME_LENGTH;
    }
}
