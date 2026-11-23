package com.vesanrebackend.dto.auth;

// Kept identical to the frontend regexes in vesanre-frontend/src/utils/auth.js.
public final class AuthPatterns {
    // Requires a dot in the domain; plain @Email accepts "a@b".
    public static final String EMAIL = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    // Vietnamese mobile: 0 or +84, then a 3/5/7/8/9 prefix and 8 digits. Empty is allowed so PATCH "" clears the phone.
    public static final String PHONE = "^$|^(0|\\+84)[35789][0-9]{8}$";
    public static final String NAME = "^(?=.{2,50}$)[\\p{L}\\p{M}]+( [\\p{L}\\p{M}]+)*$";
    public static final String PASSWORD = "^(?=.*\\p{L})(?=.*\\d)\\S{8,32}$";
    public static final String LEGAL_NAME = "^(?=.{2,200}$)[\\p{L}\\p{M}\\d .,&()\\-]+$";
    public static final String TAX_ID = "^$|^\\d{10}(-\\d{3})?$";
    public static final String SHOP_NAME = "^$|^(?=.{2,160}$)[\\p{L}\\p{M}\\d .,&()\\-]+$";
    // The frontend omits an empty comment, so null means "no comment".
    public static final String REVIEW_COMMENT = "^(?=[\\s\\S]{10,500}$)(?!([\\s\\S])\\1*$)\\S[\\s\\S]*\\S$";

    private AuthPatterns() {
    }
}
