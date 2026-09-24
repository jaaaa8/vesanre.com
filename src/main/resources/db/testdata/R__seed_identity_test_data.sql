INSERT INTO sporthub.users (
    id, email, email_normalized, password_hash, display_name, status, email_verified_at
) VALUES
    ('00000000-0000-4000-8000-000000000101', 'customer@test.sporthub.local', 'customer@test.sporthub.local', '$2a$10$6n9Hnw/SGAsp6GlpcQ3oF.yIun.C0qxpd6dTqwPNhCX27Y8mO5mjG', 'Test Customer', 'ACTIVE', CURRENT_TIMESTAMP),
    ('00000000-0000-4000-8000-000000000102', 'provider@test.sporthub.local', 'provider@test.sporthub.local', '$2a$10$6n9Hnw/SGAsp6GlpcQ3oF.yIun.C0qxpd6dTqwPNhCX27Y8mO5mjG', 'Test Provider', 'ACTIVE', CURRENT_TIMESTAMP),
    ('00000000-0000-4000-8000-000000000103', 'admin@test.sporthub.local', 'admin@test.sporthub.local', '$2a$10$6n9Hnw/SGAsp6GlpcQ3oF.yIun.C0qxpd6dTqwPNhCX27Y8mO5mjG', 'Test Admin', 'ACTIVE', CURRENT_TIMESTAMP)
ON CONFLICT (email_normalized) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    display_name = EXCLUDED.display_name,
    status = EXCLUDED.status,
    email_verified_at = EXCLUDED.email_verified_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sporthub.user_roles (user_id, role_code)
SELECT id, 'CUSTOMER' FROM sporthub.users WHERE email_normalized = 'customer@test.sporthub.local'
UNION ALL
SELECT id, 'PROVIDER' FROM sporthub.users WHERE email_normalized = 'provider@test.sporthub.local'
UNION ALL
SELECT id, 'ADMIN' FROM sporthub.users WHERE email_normalized = 'admin@test.sporthub.local'
ON CONFLICT (user_id, role_code) DO NOTHING;

INSERT INTO sporthub.provider_profiles (user_id, legal_name, status)
SELECT id, 'SportHub Test Provider', 'PENDING'
FROM sporthub.users
WHERE email_normalized = 'provider@test.sporthub.local'
ON CONFLICT (user_id) DO UPDATE SET
    legal_name = EXCLUDED.legal_name,
    status = EXCLUDED.status,
    verified_at = NULL,
    updated_at = CURRENT_TIMESTAMP;
