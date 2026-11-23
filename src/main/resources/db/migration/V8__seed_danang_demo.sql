-- Demo data for Da Nang: 1 admin, 5 providers (one shop + one venue each), 5 customers, coin wallets,
-- 10 bookings and 6 reviews. All accounts share the password Demo@123 (BCrypt cost 10).
-- Fixed UUIDs 00000000-0000-4000-9000-<n>: users 1/11-15/21-25, shops 31-35, venues 41-45, courts 100+venue*10+n,
-- wallets 201/211-215/221-225, top-ups 301-306, blocks 401-410, bookings 501-510, reviews 601-606, verifications 71-75.
-- Time anchors use CURRENT_DATE at migration time in Asia/Ho_Chi_Minh. Only past bookings are COMPLETED/CANCELLED and only
-- future bookings are CONFIRMED, so BookingCompletionJob has nothing to complete.
-- ponytail: demo seed lives in db/migration; move it to a demo-only location before a real production database exists.

CREATE FUNCTION pg_temp.seed_uuid(n integer) RETURNS uuid LANGUAGE sql IMMUTABLE AS
$$ SELECT ('00000000-0000-4000-9000-' || lpad(n::text, 12, '0'))::uuid $$;

-- Accounts -------------------------------------------------------------------------------------------------------
INSERT INTO sporthub.users (id, email, email_normalized, password_hash, display_name, phone, status, email_verified_at) VALUES
    (pg_temp.seed_uuid(1),  'admin@vesanre.demo',   'admin@vesanre.demo',   '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', 'Quản trị viên Vesanre', '0905000001', 'ACTIVE', now()),
    (pg_temp.seed_uuid(11), 'chusan1@vesanre.demo', 'chusan1@vesanre.demo', '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', 'Nguyễn Văn Hải',        '0905000011', 'ACTIVE', now()),
    (pg_temp.seed_uuid(12), 'chusan2@vesanre.demo', 'chusan2@vesanre.demo', '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', 'Trần Quốc Bảo',         '0905000012', 'ACTIVE', now()),
    (pg_temp.seed_uuid(13), 'chusan3@vesanre.demo', 'chusan3@vesanre.demo', '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', 'Lê Thị Mỹ Linh',        '0905000013', 'ACTIVE', now()),
    (pg_temp.seed_uuid(14), 'chusan4@vesanre.demo', 'chusan4@vesanre.demo', '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', 'Phạm Đức Thịnh',        '0905000014', 'ACTIVE', now()),
    (pg_temp.seed_uuid(15), 'chusan5@vesanre.demo', 'chusan5@vesanre.demo', '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', 'Võ Thanh Tùng',         '0905000015', 'ACTIVE', now()),
    (pg_temp.seed_uuid(21), 'khach1@vesanre.demo',  'khach1@vesanre.demo',  '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', 'Đặng Minh Khoa',        '0905000021', 'ACTIVE', now()),
    (pg_temp.seed_uuid(22), 'khach2@vesanre.demo',  'khach2@vesanre.demo',  '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', 'Huỳnh Ngọc Anh',        '0905000022', 'ACTIVE', now()),
    (pg_temp.seed_uuid(23), 'khach3@vesanre.demo',  'khach3@vesanre.demo',  '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', 'Bùi Quang Vinh',        '0905000023', 'ACTIVE', now()),
    (pg_temp.seed_uuid(24), 'khach4@vesanre.demo',  'khach4@vesanre.demo',  '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', 'Ngô Thùy Dương',        '0905000024', 'ACTIVE', now()),
    (pg_temp.seed_uuid(25), 'khach5@vesanre.demo',  'khach5@vesanre.demo',  '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', 'Đỗ Gia Huy',            '0905000025', 'ACTIVE', now());

-- Every account has CUSTOMER (registration); providers also hold PROVIDER (granted by the admin on approval).
INSERT INTO sporthub.user_roles (user_id, role_code, granted_by)
SELECT pg_temp.seed_uuid(n), 'CUSTOMER', NULL FROM unnest(ARRAY[1, 11, 12, 13, 14, 15, 21, 22, 23, 24, 25]) AS n;
INSERT INTO sporthub.user_roles (user_id, role_code, granted_by) VALUES (pg_temp.seed_uuid(1), 'ADMIN', NULL);
INSERT INTO sporthub.user_roles (user_id, role_code, granted_by)
SELECT pg_temp.seed_uuid(10 + k), 'PROVIDER', pg_temp.seed_uuid(1) FROM generate_series(1, 5) AS k;

-- Provider side: VERIFIED profile, approved verification, one ACTIVE shop each -------------------------------------
INSERT INTO sporthub.provider_profiles (user_id, legal_name, status, verified_at) VALUES
    (pg_temp.seed_uuid(11), 'Hộ kinh doanh Hải Âu Sport',          'VERIFIED', now() - interval '60 days'),
    (pg_temp.seed_uuid(12), 'Công ty TNHH Thể thao Biển Xanh',     'VERIFIED', now() - interval '60 days'),
    (pg_temp.seed_uuid(13), 'Hộ kinh doanh Ngũ Hành Pickle',       'VERIFIED', now() - interval '60 days'),
    (pg_temp.seed_uuid(14), 'Công ty TNHH Tennis Cẩm Lệ',          'VERIFIED', now() - interval '60 days'),
    (pg_temp.seed_uuid(15), 'Công ty TNHH Sân bóng Thanh Khê',     'VERIFIED', now() - interval '60 days');

INSERT INTO sporthub.provider_verifications (id, submitted_by, documents, status, reviewed_by, reviewed_at, provider_user_id,
                                             created_at, updated_at)
SELECT pg_temp.seed_uuid(70 + k), pg_temp.seed_uuid(10 + k), '[]'::jsonb, 'APPROVED', pg_temp.seed_uuid(1),
       now() - interval '60 days', pg_temp.seed_uuid(10 + k), now() - interval '61 days', now() - interval '60 days'
FROM generate_series(1, 5) AS k;

INSERT INTO sporthub.shops (id, owner_user_id, slug, name, description, status) VALUES
    (pg_temp.seed_uuid(31), pg_temp.seed_uuid(11), 'hai-au-sport',          'Hải Âu Sport',          'Cụm sân cầu lông trung tâm quận Hải Châu.',                  'ACTIVE'),
    (pg_temp.seed_uuid(32), pg_temp.seed_uuid(12), 'bien-xanh-arena',       'Biển Xanh Arena',       'Sân bóng đá 7 người gần biển Mỹ Khê, quận Sơn Trà.',         'ACTIVE'),
    (pg_temp.seed_uuid(33), pg_temp.seed_uuid(13), 'ngu-hanh-pickle',       'Ngũ Hành Pickle',       'Sân pickleball ngoài trời khu Ngũ Hành Sơn.',                'ACTIVE'),
    (pg_temp.seed_uuid(34), pg_temp.seed_uuid(14), 'cam-le-tennis-club',    'Cẩm Lệ Tennis Club',    'Câu lạc bộ tennis với 3 sân mặt cứng tại quận Cẩm Lệ.',      'ACTIVE'),
    (pg_temp.seed_uuid(35), pg_temp.seed_uuid(15), 'thanh-khe-mini-stadium','Thanh Khê Mini Stadium','Sân bóng đá 5 người cỏ nhân tạo tại quận Thanh Khê.',       'ACTIVE');

-- Venues: one row per venue in the spec table ----------------------------------------------------------------------
CREATE TEMP TABLE seed_venue ON COMMIT DROP AS
SELECT * FROM (VALUES
    (1, 'BADMINTON',   6,  5, 22,  60000,  90000,  0),
    (2, 'FOOTBALL_7',  2,  6, 23, 400000, 600000, 30),
    (3, 'PICKLEBALL',  4,  5, 22, 120000, 160000,  0),
    (4, 'TENNIS',      3,  5, 21, 150000, 200000, 20),
    (5, 'FOOTBALL_5',  3,  6, 23, 250000, 350000,  0)
) AS t(v, sport, courts, open_h, close_h, normal_price, peak_price, deposit);

INSERT INTO sporthub.venues (id, shop_id, slug, name, description, address_line, ward, district, city, province,
                             latitude, longitude, phone, status, deposit_percent) VALUES
    (pg_temp.seed_uuid(41), pg_temp.seed_uuid(31), 'cau-long-hai-au',        'Cầu lông Hải Âu',
     '6 sân cầu lông thảm tiêu chuẩn, sàn chống trơn, đèn LED không chói.',
     '45 Nguyễn Văn Linh', 'Phước Ninh', 'Hải Châu', 'Đà Nẵng', 'Đà Nẵng', 16.059800, 108.213600, '02363800001', 'ACTIVE', 0),
    (pg_temp.seed_uuid(42), pg_temp.seed_uuid(32), 'san-bong-7-bien-xanh',   'Sân bóng đá 7 Biển Xanh',
     '2 sân cỏ nhân tạo 7 người, có mái che khán đài, cách biển Mỹ Khê vài phút đi bộ.',
     '120 Võ Nguyên Giáp', 'Phước Mỹ', 'Sơn Trà', 'Đà Nẵng', 'Đà Nẵng', 16.060400, 108.246800, '02363800002', 'ACTIVE', 30),
    (pg_temp.seed_uuid(43), pg_temp.seed_uuid(33), 'pickleball-ngu-hanh',    'Pickleball Ngũ Hành',
     '4 sân pickleball tiêu chuẩn, cho thuê vợt và bóng tại quầy.',
     '88 Lê Văn Hiến', 'Khuê Mỹ', 'Ngũ Hành Sơn', 'Đà Nẵng', 'Đà Nẵng', 16.031500, 108.254900, '02363800003', 'ACTIVE', 0),
    (pg_temp.seed_uuid(44), pg_temp.seed_uuid(34), 'tennis-cam-le-xanh',     'Tennis Cẩm Lệ Xanh',
     '3 sân tennis mặt cứng, có đèn chiếu sáng buổi tối.',
     '210 Cách Mạng Tháng Tám', 'Hòa Thọ Tây', 'Cẩm Lệ', 'Đà Nẵng', 'Đà Nẵng', 16.025600, 108.201800, '02363800004', 'ACTIVE', 20),
    (pg_temp.seed_uuid(45), pg_temp.seed_uuid(35), 'san-bong-5-thanh-khe',   'Sân bóng đá 5 Thanh Khê',
     '3 sân cỏ nhân tạo 5 người, bãi giữ xe rộng.',
     '67 Hà Huy Tập', 'Xuân Hà', 'Thanh Khê', 'Đà Nẵng', 'Đà Nẵng', 16.071200, 108.190500, '02363800005', 'ACTIVE', 0);

INSERT INTO sporthub.venue_amenities (venue_id, amenity_id, details)
SELECT pg_temp.seed_uuid(40 + a.v), am.id, a.details
FROM (VALUES
    (1, 'PARKING', 'Bãi xe máy miễn phí'), (1, 'LOCKER_ROOM', NULL), (1, 'WIFI', NULL),
    (2, 'PARKING', 'Bãi xe ô tô và xe máy'), (2, 'SHOWER', NULL), (2, 'LOCKER_ROOM', NULL), (2, 'EQUIPMENT_RENTAL', 'Cho thuê áo bib'),
    (3, 'PARKING', NULL), (3, 'EQUIPMENT_RENTAL', 'Cho thuê vợt pickleball'), (3, 'WIFI', NULL),
    (4, 'PARKING', NULL), (4, 'SHOWER', NULL), (4, 'LOCKER_ROOM', NULL), (4, 'EQUIPMENT_RENTAL', 'Cho thuê vợt tennis'),
    (5, 'PARKING', 'Bãi xe rộng'), (5, 'SHOWER', NULL), (5, 'WIFI', NULL)
) AS a(v, code, details)
JOIN sporthub.amenities am ON am.code = a.code;

-- Courts S1..Sn per venue; identical sport, hours (all 7 days) and price list, as VenueSetupService writes them ------
CREATE TEMP TABLE seed_court ON COMMIT DROP AS
SELECT sv.v, c AS n, pg_temp.seed_uuid(100 + sv.v * 10 + c) AS id
FROM seed_venue sv CROSS JOIN LATERAL generate_series(1, sv.courts) AS c;

INSERT INTO sporthub.courts (id, venue_id, code, name, capacity, booking_step_minutes, min_booking_minutes, max_booking_minutes, status)
SELECT sc.id, pg_temp.seed_uuid(40 + sc.v), 'S' || sc.n, 'Sân ' || sc.n, 4, 60, 60, 120, 'ACTIVE' FROM seed_court sc;

INSERT INTO sporthub.court_sports (court_id, sport_id, is_primary)
SELECT sc.id, sp.id, true FROM seed_court sc
JOIN seed_venue sv ON sv.v = sc.v JOIN sporthub.sports sp ON sp.code = sv.sport;

INSERT INTO sporthub.court_operating_hours (court_id, weekday, opens_at, closes_at, is_closed)
SELECT sc.id, wd, make_time(sv.open_h, 0, 0), make_time(sv.close_h, 0, 0), false
FROM seed_court sc JOIN seed_venue sv ON sv.v = sc.v CROSS JOIN generate_series(0, 6) AS wd;

-- Normal price until 17h, peak 17h-22h (or closing time), normal again after 22h.
INSERT INTO sporthub.court_pricing_rules (id, court_id, weekday, start_minute, end_minute, price_per_hour, currency, is_active)
SELECT md5('seed-rule-' || sc.id || '-' || wd || '-' || r.start_minute)::uuid, sc.id, wd, r.start_minute, r.end_minute, r.price, 'VND', true
FROM seed_court sc JOIN seed_venue sv ON sv.v = sc.v CROSS JOIN generate_series(0, 6) AS wd
CROSS JOIN LATERAL (VALUES
    (sv.open_h * 60, 17 * 60, sv.normal_price),
    (17 * 60, LEAST(22, sv.close_h) * 60, sv.peak_price),
    (22 * 60, sv.close_h * 60, sv.normal_price)
) AS r(start_minute, end_minute, price)
WHERE r.start_minute < r.end_minute;

-- Wallets and the top-ups that fund them ---------------------------------------------------------------------------
INSERT INTO sporthub.coin_wallets (id, user_id, shop_id, balance) VALUES
    (pg_temp.seed_uuid(201), pg_temp.seed_uuid(1),  NULL, 0),
    (pg_temp.seed_uuid(211), pg_temp.seed_uuid(21), NULL, 0),
    (pg_temp.seed_uuid(212), pg_temp.seed_uuid(22), NULL, 0),
    (pg_temp.seed_uuid(213), pg_temp.seed_uuid(23), NULL, 0),
    (pg_temp.seed_uuid(214), pg_temp.seed_uuid(24), NULL, 0),
    (pg_temp.seed_uuid(215), pg_temp.seed_uuid(25), NULL, 0),
    (pg_temp.seed_uuid(221), NULL, pg_temp.seed_uuid(31), 0),
    (pg_temp.seed_uuid(222), NULL, pg_temp.seed_uuid(32), 0),
    (pg_temp.seed_uuid(223), NULL, pg_temp.seed_uuid(33), 0),
    (pg_temp.seed_uuid(224), NULL, pg_temp.seed_uuid(34), 0),
    (pg_temp.seed_uuid(225), NULL, pg_temp.seed_uuid(35), 0);

CREATE TEMP TABLE seed_topup ON COMMIT DROP AS
SELECT * FROM (VALUES
    (1, 201, 100000), (2, 211, 500), (3, 212, 450), (4, 213, 300), (5, 214, 300), (6, 215, 200)
) AS t(n, wallet_no, coins);

INSERT INTO sporthub.coin_top_ups (id, wallet_id, transfer_code, coin_amount, amount_vnd, status, provider_code,
                                   provider_transaction_id, expires_at, paid_at, created_at, updated_at)
SELECT pg_temp.seed_uuid(300 + t.n), pg_temp.seed_uuid(t.wallet_no), 'SEEDTOPUP' || lpad(t.n::text, 2, '0'), t.coins,
       t.coins * 1000, 'PAID', 'SEED', 'seed-' || t.n,
       now() - interval '20 days' + interval '30 minutes', now() - interval '20 days' + interval '5 minutes',
       now() - interval '20 days', now() - interval '20 days' + interval '5 minutes'
FROM seed_topup t;

-- Bookings: 6 COMPLETED (past), 3 CONFIRMED (future; two fill both courts of venue 2 tomorrow 19-20h), 1 CANCELLED -----
CREATE TEMP TABLE seed_bk ON COMMIT DROP AS
SELECT b.*, pg_temp.seed_uuid(500 + b.n) AS booking_id, pg_temp.seed_uuid(400 + b.n) AS block_id,
       pg_temp.seed_uuid(100 + b.v * 10 + b.c) AS court_id, sv.deposit,
       ((CURRENT_DATE + b.day_off)::timestamp + make_time(b.hs, 0, 0)) AT TIME ZONE 'Asia/Ho_Chi_Minh' AS start_at,
       ((CURRENT_DATE + b.day_off)::timestamp + make_time(b.he, 0, 0)) AT TIME ZONE 'Asia/Ho_Chi_Minh' AS end_at,
       p.lines, p.subtotal
FROM (VALUES
    (1,  1, 1, 1, -14, 18, 19, 'COMPLETED'),
    (2,  2, 2, 1, -10, 19, 21, 'COMPLETED'),
    (3,  3, 3, 2,  -8,  7,  8, 'COMPLETED'),
    (4,  4, 4, 1,  -6, 16, 18, 'COMPLETED'),
    (5,  5, 5, 2,  -5, 19, 20, 'COMPLETED'),
    (6,  1, 2, 2,  -3, 20, 21, 'COMPLETED'),
    (7,  3, 2, 1,   1, 19, 20, 'CONFIRMED'),
    (8,  4, 2, 2,   1, 19, 20, 'CONFIRMED'),
    (9,  5, 1, 3,   2, 18, 19, 'CONFIRMED'),
    (10, 2, 4, 2,   3, 17, 18, 'CANCELLED')
) AS b(n, cust, v, c, day_off, hs, he, status)
JOIN seed_venue sv ON sv.v = b.v
CROSS JOIN LATERAL (
    SELECT jsonb_agg(jsonb_build_object('start', to_char(make_time(h, 0, 0), 'HH24:MI:SS'),
                                        'end', to_char(make_time(h + 1, 0, 0), 'HH24:MI:SS'),
                                        'price', r.price_per_hour) ORDER BY h) AS lines,
           sum(r.price_per_hour) AS subtotal
    FROM generate_series(b.hs, b.he - 1) AS h
    JOIN sporthub.court_pricing_rules r
      ON r.court_id = pg_temp.seed_uuid(100 + b.v * 10 + b.c) AND r.weekday = 0
     AND h * 60 >= r.start_minute AND h * 60 < r.end_minute
) p;

ALTER TABLE seed_bk ADD COLUMN coins bigint, ADD COLUMN created_at timestamptz, ADD COLUMN completed_at timestamptz,
    ADD COLUMN cancelled_at timestamptz;
UPDATE seed_bk SET
    coins = floor(subtotal * deposit / 100 / 1000),
    created_at = CASE status WHEN 'COMPLETED' THEN start_at - interval '2 days'
                             WHEN 'CANCELLED' THEN now() - interval '2 days'
                             ELSE now() - interval '1 day' END,
    completed_at = CASE WHEN status = 'COMPLETED' THEN end_at + interval '1 minute' END,
    cancelled_at = CASE WHEN status = 'CANCELLED' THEN now() - interval '1 day' END;

INSERT INTO sporthub.court_schedule_blocks (id, court_id, block_kind, state, start_at, end_at, created_by, released_at, created_at)
SELECT block_id, court_id, 'BOOKING', CASE WHEN status = 'CANCELLED' THEN 'RELEASED' ELSE 'ACTIVE' END, start_at, end_at,
       pg_temp.seed_uuid(20 + cust), cancelled_at, created_at
FROM seed_bk;

INSERT INTO sporthub.bookings (id, booking_code, customer_id, schedule_block_id, status, shop_name_snapshot, venue_name_snapshot,
                               court_name_snapshot, unit_price_snapshot, subtotal_amount, deposit_amount, deposit_coins, currency,
                               price_breakdown_snapshot, cancellation_policy_snapshot, cancellation_source, cancelled_by,
                               cancellation_reason, created_at, confirmed_at, cancelled_at, completed_at)
SELECT k.booking_id, 'BK' || to_char(k.created_at AT TIME ZONE 'UTC', 'YYMMDD') || 'DEMO' || lpad(k.n::text, 2, '0'),
       pg_temp.seed_uuid(20 + k.cust), k.block_id, k.status, s.name, v.name, 'Sân ' || k.c,
       round(k.subtotal * 60 / ((k.he - k.hs) * 60)), k.subtotal, k.coins * 1000, k.coins, 'VND', k.lines,
       jsonb_build_object('depositPercent', k.deposit, 'depositReturnedOnCancel', true),
       CASE WHEN k.status = 'CANCELLED' THEN 'CUSTOMER' END,
       CASE WHEN k.status = 'CANCELLED' THEN pg_temp.seed_uuid(20 + k.cust) END,
       CASE WHEN k.status = 'CANCELLED' THEN 'Bận việc đột xuất, xin hủy lịch.' END,
       k.created_at, k.created_at, k.cancelled_at, k.completed_at
FROM seed_bk k
JOIN sporthub.venues v ON v.id = pg_temp.seed_uuid(40 + k.v)
JOIN sporthub.shops s ON s.id = v.shop_id;

INSERT INTO sporthub.booking_status_history (booking_id, from_status, to_status, actor_type, actor_user_id, reason_code, metadata, created_at)
SELECT booking_id, NULL, 'CONFIRMED', 'CUSTOMER', pg_temp.seed_uuid(20 + cust), 'BOOKED', jsonb_build_object('coins', coins), created_at
FROM seed_bk
UNION ALL
SELECT booking_id, 'CONFIRMED', 'COMPLETED', 'SYSTEM', NULL, 'COMPLETED', jsonb_build_object('coins', coins), completed_at
FROM seed_bk WHERE status = 'COMPLETED'
UNION ALL
SELECT booking_id, 'CONFIRMED', 'CANCELLED', 'CUSTOMER', pg_temp.seed_uuid(20 + cust), 'CANCELLED_BY_CUSTOMER',
       jsonb_build_object('coins', coins), cancelled_at
FROM seed_bk WHERE status = 'CANCELLED'
ORDER BY 8;

-- Ledger: top-ups, deposits, payouts to the shop, returned deposit; balance_after is the running sum per wallet ------
CREATE TEMP TABLE seed_ledger ON COMMIT DROP AS
SELECT pg_temp.seed_uuid(t.wallet_no) AS wallet_id, 'TOP_UP' AS entry_type, t.coins AS amount,
       pg_temp.seed_uuid(300 + t.n) AS top_up_id, NULL::uuid AS booking_id, now() - interval '20 days' + interval '5 minutes' AS created_at
FROM seed_topup t
UNION ALL
SELECT pg_temp.seed_uuid(210 + cust), 'BOOKING_DEPOSIT', -coins, NULL, booking_id, created_at FROM seed_bk WHERE coins > 0
UNION ALL
SELECT pg_temp.seed_uuid(220 + v), 'BOOKING_PAYOUT', coins, NULL, booking_id, completed_at FROM seed_bk WHERE status = 'COMPLETED' AND coins > 0
UNION ALL
SELECT pg_temp.seed_uuid(210 + cust), 'BOOKING_DEPOSIT_RETURN', coins, NULL, booking_id, cancelled_at FROM seed_bk WHERE status = 'CANCELLED' AND coins > 0;

INSERT INTO sporthub.coin_ledger_entries (wallet_id, entry_type, amount, balance_after, top_up_id, booking_id, created_at)
SELECT wallet_id, entry_type, amount, sum(amount) OVER (PARTITION BY wallet_id ORDER BY created_at), top_up_id, booking_id, created_at
FROM seed_ledger ORDER BY created_at;

UPDATE sporthub.coin_wallets w
SET balance = (SELECT COALESCE(sum(l.amount), 0) FROM seed_ledger l WHERE l.wallet_id = w.id)
WHERE w.id IN (SELECT wallet_id FROM seed_ledger);

-- Reviews on the 6 COMPLETED bookings; two carry a reply from the venue owner --------------------------------------
INSERT INTO sporthub.reviews (id, booking_id, customer_id, rating, comment, provider_reply, replied_by, replied_at, status,
                              created_at, updated_at)
SELECT pg_temp.seed_uuid(600 + r.n), k.booking_id, pg_temp.seed_uuid(20 + k.cust), r.rating, r.comment, r.reply,
       CASE WHEN r.reply IS NOT NULL THEN pg_temp.seed_uuid(10 + k.v) END,
       CASE WHEN r.reply IS NOT NULL THEN k.completed_at + interval '1 day' END,
       'PUBLISHED', k.completed_at + interval '3 hours', k.completed_at + interval '3 hours'
FROM (VALUES
    (1, 5, 'Sân sạch, đèn sáng đều, đánh rất thoải mái.', NULL),
    (2, 4, 'Cỏ nhân tạo tốt, đá tối mát mẻ. Hơi đông vào giờ cao điểm.',
        'Cảm ơn bạn đã ủng hộ, bên mình sẽ bố trí thêm người hỗ trợ giờ cao điểm.'),
    (3, 5, 'Giá hợp lý, nhân viên nhiệt tình, có cho thuê vợt.', NULL),
    (4, 3, 'Mặt sân ổn nhưng lưới hơi chùng, mong chủ sân kiểm tra lại.',
        'Cảm ơn góp ý, bên mình đã thay lưới mới cho sân số 1.'),
    (5, 4, 'Sân rộng, bãi giữ xe thuận tiện, sẽ quay lại.', NULL),
    (6, 5, 'Đá buổi tối rất đã, chủ sân thân thiện.', NULL)
) AS r(n, rating, comment, reply)
JOIN seed_bk k ON k.n = r.n;
