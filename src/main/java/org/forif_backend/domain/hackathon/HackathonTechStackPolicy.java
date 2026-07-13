package org.forif_backend.domain.hackathon;

import java.util.List;
import java.util.Set;

public final class HackathonTechStackPolicy {

    public static final int MAX_COUNT = 4;

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
            "GitHub",
            "기타"
    );

    private static final Set<String> OPTION_SET = Set.copyOf(OPTIONS);

    private HackathonTechStackPolicy() {
    }

    public static boolean isAllowed(String techStack) {
        return OPTION_SET.contains(techStack);
    }
}
