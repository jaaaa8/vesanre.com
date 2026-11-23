-- Run after V9 with: psql -f seed-check.sql. Every query must return 0 rows.

-- 1. Wallets whose balance differs from the sum of their ledger.
SELECT w.id, w.balance, COALESCE(sum(l.amount), 0) AS ledger_sum
FROM sporthub.coin_wallets w LEFT JOIN sporthub.coin_ledger_entries l ON l.wallet_id = w.id
GROUP BY w.id, w.balance HAVING w.balance <> COALESCE(sum(l.amount), 0);

-- 1b. Ledger entries whose balance_after is not the running sum.
SELECT id, wallet_id FROM (
    SELECT id, wallet_id, balance_after, sum(amount) OVER (PARTITION BY wallet_id ORDER BY id) AS running
    FROM sporthub.coin_ledger_entries) t
WHERE balance_after <> running;

-- 2. Reviews whose booking is not COMPLETED.
SELECT r.id FROM sporthub.reviews r JOIN sporthub.bookings b ON b.id = r.booking_id WHERE b.status <> 'COMPLETED';

-- 3. Seed venues that are not ACTIVE or whose ACTIVE court count differs from the spec table.
SELECT e.venue_no, e.expected, count(c.id) AS actual
FROM (VALUES (41, 6), (42, 2), (43, 4), (44, 3), (45, 3)) AS e(venue_no, expected)
LEFT JOIN sporthub.venues v ON v.id = ('00000000-0000-4000-9000-' || lpad(e.venue_no::text, 12, '0'))::uuid AND v.status = 'ACTIVE'
LEFT JOIN sporthub.courts c ON c.venue_id = v.id AND c.status = 'ACTIVE'
GROUP BY e.venue_no, e.expected HAVING count(c.id) <> e.expected;

-- 4. @vesanre.demo accounts per role (a provider also holds CUSTOMER, so CUSTOMER counts accounts without PROVIDER/ADMIN).
SELECT role, n, expected FROM (
    SELECT 'ADMIN' AS role, 1 AS expected,
           (SELECT count(*) FROM sporthub.users u JOIN sporthub.user_roles r ON r.user_id = u.id AND r.role_code = 'ADMIN'
            WHERE u.email LIKE '%@vesanre.demo') AS n
    UNION ALL
    SELECT 'PROVIDER', 20, (SELECT count(*) FROM sporthub.users u JOIN sporthub.user_roles r ON r.user_id = u.id AND r.role_code = 'PROVIDER'
                           WHERE u.email LIKE '%@vesanre.demo')
    UNION ALL
    SELECT 'CUSTOMER', 5, (SELECT count(*) FROM sporthub.users u
                           WHERE u.email LIKE '%@vesanre.demo'
                             AND EXISTS (SELECT 1 FROM sporthub.user_roles r WHERE r.user_id = u.id AND r.role_code = 'CUSTOMER')
                             AND NOT EXISTS (SELECT 1 FROM sporthub.user_roles r WHERE r.user_id = u.id AND r.role_code IN ('PROVIDER', 'ADMIN')))
) t WHERE n <> expected;

-- 9a. Seed cities that do not have exactly 10 ACTIVE venues, plus any expected city that is missing.
SELECT city, count(*) FROM sporthub.venues
WHERE id::text LIKE '00000000-0000-4000-9000-%' AND status = 'ACTIVE'
GROUP BY city HAVING count(*) <> 10
UNION ALL
SELECT e.city, 0 FROM (VALUES ('Đà Nẵng'), ('Hồ Chí Minh')) AS e(city)
WHERE NOT EXISTS (SELECT 1 FROM sporthub.venues v WHERE v.city = e.city AND v.id::text LIKE '00000000-0000-4000-9000-%' AND v.status = 'ACTIVE');

-- 9b. Seed venues without any court.
SELECT v.id FROM sporthub.venues v
WHERE v.id::text LIKE '00000000-0000-4000-9000-%'
  AND NOT EXISTS (SELECT 1 FROM sporthub.courts c WHERE c.venue_id = v.id);

-- 9c. Seed courts missing operating hours or a pricing rule on some weekday.
SELECT c.id, wd FROM sporthub.courts c CROSS JOIN generate_series(0, 6) AS wd
WHERE c.id::text LIKE '00000000-0000-4000-9000-%'
  AND (NOT EXISTS (SELECT 1 FROM sporthub.court_operating_hours h WHERE h.court_id = c.id AND h.weekday = wd)
    OR NOT EXISTS (SELECT 1 FROM sporthub.court_pricing_rules r WHERE r.court_id = c.id AND r.weekday = wd));
