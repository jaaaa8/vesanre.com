package com.vesanrebackend.dto.auth;

// Kept identical to the frontend regexes in vesanre-frontend/src/utils/auth.js.
public final class AuthPatterns {
    // Requires a dot in the domain; plain @Email accepts "a@b".
    public static final String EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    // Vietnamese mobile: 0 or +84, then a 3/5/7/8/9 prefix and 8 digits. Empty is allowed so PATCH "" clears the phone.
    public static final String PHONE = "^$|^(0|\\+84)[35789][0-9]{8}$";

    private AuthPatterns() {
    }
}
