package com.vesanrebackend.util;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;

public final class SlugGenerator {
    private static final String SLUG_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private SlugGenerator() {
    }

    // Slug = accent-stripped lowercase name + random suffix, so two rows with the same name never collide.
    public static String generate(String name, String fallback) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'd')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-");
        base = base.substring(0, Math.min(base.length(), 100)).replaceAll("^-+|-+$", "");
        if (base.isEmpty()) {
            base = fallback;
        }
        StringBuilder suffix = new StringBuilder("-");
        for (int i = 0; i < 6; i++) {
            suffix.append(SLUG_CHARS.charAt(RANDOM.nextInt(SLUG_CHARS.length())));
        }
        return base + suffix;
    }
}
