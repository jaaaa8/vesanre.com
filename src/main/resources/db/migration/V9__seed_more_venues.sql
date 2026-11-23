-- More demo venues: 5 in Da Nang (10 in total with V8) and 10 in Ho Chi Minh City, one provider + shop + venue each.
-- Same shape as V8; no bookings or reviews. Password Demo@123 (BCrypt cost 10). Pre-2025-07-01 administrative units.
-- Fixed UUIDs 00000000-0000-4000-9000-<n>, k = 1..15: users 1100+k, shops 1200+k, venues 1300+k, verifications 1400+k,
-- shop wallets 1500+k, courts 2000+k*10+n.
-- ponytail: demo seed lives in db/migration; move it to a demo-only location before a real production database exists.

-- V8 may have created it in the same session.
CREATE OR REPLACE FUNCTION pg_temp.seed_uuid(n integer) RETURNS uuid LANGUAGE sql IMMUTABLE AS
$$ SELECT ('00000000-0000-4000-9000-' || lpad(n::text, 12, '0'))::uuid $$;

CREATE TEMP TABLE seed9_venue ON COMMIT DROP AS
SELECT * FROM (VALUES
    (1,  'Trương Văn Phúc', 'Hòa Khánh Badminton',     'hoa-khanh-badminton',     'Cầu lông Hòa Khánh',            'cau-long-hoa-khanh',            'BADMINTON',  5, 5, 22,  60000,  90000,  0, '15 Nguyễn Lương Bằng',   'Hòa Khánh Bắc', 'Liên Chiểu',   'Đà Nẵng',     16.074000, 108.150000, '02363800006'),
    (2,  'Lý Thị Hạnh',     'Hòa Xuân Football',       'hoa-xuan-football',       'Sân bóng đá 5 Hòa Xuân',        'san-bong-da-5-hoa-xuan',        'FOOTBALL_5', 3, 6, 23, 250000, 350000,  0, '42 Nguyễn Phước Lan',    'Hòa Xuân',      'Cẩm Lệ',       'Đà Nẵng',     16.005000, 108.225000, '02363800007'),
    (3,  'Hồ Minh Tâm',     'Mỹ An Pickle',            'my-an-pickle',            'Pickleball Mỹ An',              'pickleball-my-an',              'PICKLEBALL', 4, 5, 22, 120000, 160000,  0, '25 Hồ Xuân Hương',       'Mỹ An',         'Ngũ Hành Sơn', 'Đà Nẵng',     16.048000, 108.244000, '02363800008'),
    (4,  'Châu Ngọc Lan',   'An Hải Tennis',           'an-hai-tennis',           'Tennis An Hải',                 'tennis-an-hai',                 'TENNIS',     2, 5, 21, 150000, 200000, 20, '50 Ngô Quyền',           'An Hải Bắc',    'Sơn Trà',      'Đà Nẵng',     16.070000, 108.232000, '02363800009'),
    (5,  'Mai Xuân Trường', 'Hòa Cường Arena',         'hoa-cuong-arena',         'Sân bóng đá 7 Hòa Cường',       'san-bong-da-7-hoa-cuong',       'FOOTBALL_7', 2, 6, 23, 400000, 600000, 30, '100 Núi Thành',          'Hòa Cường Bắc', 'Hải Châu',     'Đà Nẵng',     16.043000, 108.216000, '02363800010'),
    (6,  'Tạ Hoàng Nam',    'Bến Thành Badminton',     'ben-thanh-badminton',     'Cầu lông Bến Thành',            'cau-long-ben-thanh',            'BADMINTON',  6, 5, 22,  70000, 110000,  0, '1 Huyền Trân Công Chúa', 'Bến Thành',     'Quận 1',       'Hồ Chí Minh', 10.774000, 106.693000, '02838000011'),
    (7,  'Dương Thị Thu',   'Lý Thường Kiệt Arena',    'ly-thuong-kiet-arena',    'Sân bóng đá 7 Lý Thường Kiệt',  'san-bong-da-7-ly-thuong-kiet',  'FOOTBALL_7', 2, 6, 23, 500000, 750000, 30, '219 Lý Thường Kiệt',     'Phường 15',     'Quận 11',      'Hồ Chí Minh', 10.768000, 106.656000, '02838000012'),
    (8,  'Lâm Quốc Việt',   'Thảo Điền Tennis Club',   'thao-dien-tennis-club',   'Tennis Thảo Điền',              'tennis-thao-dien',              'TENNIS',     3, 5, 21, 180000, 250000, 20, '20 Quốc Hương',          'Thảo Điền',     'Thủ Đức',      'Hồ Chí Minh', 10.804000, 106.736000, '02838000013'),
    (9,  'Kiều Anh Thư',    'Phú Mỹ Hưng Pickle',      'phu-my-hung-pickle',      'Pickleball Phú Mỹ Hưng',        'pickleball-phu-my-hung',        'PICKLEBALL', 4, 5, 22, 140000, 180000,  0, '30 Tôn Dật Tiên',        'Tân Phong',     'Quận 7',       'Hồ Chí Minh', 10.729000, 106.704000, '02838000014'),
    (10, 'Phan Thế Vinh',   'Điện Biên Phủ Football',  'dien-bien-phu-football',  'Sân bóng đá 5 Điện Biên Phủ',   'san-bong-da-5-dien-bien-phu',   'FOOTBALL_5', 3, 6, 23, 300000, 420000,  0, '180 Điện Biên Phủ',      'Phường 25',     'Bình Thạnh',   'Hồ Chí Minh', 10.800000, 106.715000, '02838000015'),
    (11, 'Vũ Hồng Nhung',   'Cộng Hòa Badminton',      'cong-hoa-badminton',      'Cầu lông Cộng Hòa',             'cau-long-cong-hoa',             'BADMINTON',  5, 5, 22,  70000, 100000,  0, '35 Cộng Hòa',            'Phường 4',      'Tân Bình',     'Hồ Chí Minh', 10.801000, 106.652000, '02838000016'),
    (12, 'Cao Minh Đức',    'Quang Trung Arena',       'quang-trung-arena',       'Sân bóng đá 7 Quang Trung',     'san-bong-da-7-quang-trung',     'FOOTBALL_7', 2, 6, 23, 450000, 700000, 30, '400 Quang Trung',        'Phường 10',     'Gò Vấp',       'Hồ Chí Minh', 10.834000, 106.666000, '02838000017'),
    (13, 'Tô Thị Kim',      'Phú Nhuận Tennis',        'phu-nhuan-tennis',        'Tennis Phan Đăng Lưu',          'tennis-phan-dang-luu',          'TENNIS',     2, 5, 21, 160000, 220000, 20, '120 Phan Đăng Lưu',      'Phường 3',      'Phú Nhuận',    'Hồ Chí Minh', 10.801000, 106.685000, '02838000018'),
    (14, 'Lưu Đình Khang',  'Quận 3 Pickle',           'quan-3-pickle',           'Pickleball Trần Quốc Thảo',     'pickleball-tran-quoc-thao',     'PICKLEBALL', 3, 5, 22, 130000, 170000,  0, '50 Trần Quốc Thảo',      'Võ Thị Sáu',    'Quận 3',       'Hồ Chí Minh', 10.783000, 106.688000, '02838000019'),
    (15, 'Hà Bảo Ngọc',     'Linh Chiểu Football',     'linh-chieu-football',     'Sân bóng đá 5 Võ Văn Ngân',     'san-bong-da-5-vo-van-ngan',     'FOOTBALL_5', 4, 6, 23, 280000, 400000,  0, '10 Võ Văn Ngân',         'Linh Chiểu',    'Thủ Đức',      'Hồ Chí Minh', 10.850000, 106.762000, '02838000020')
) AS t(k, owner_name, shop_name, shop_slug, venue_name, venue_slug, sport, courts, open_h, close_h, normal_price, peak_price,
       deposit, address_line, ward, district, city, latitude, longitude, venue_phone);

-- Vietnamese sport label used in the descriptions.
ALTER TABLE seed9_venue ADD COLUMN sport_label text;
UPDATE seed9_venue SET sport_label = CASE sport
    WHEN 'BADMINTON' THEN 'cầu lông' WHEN 'FOOTBALL_5' THEN 'bóng đá 5 người' WHEN 'FOOTBALL_7' THEN 'bóng đá 7 người'
    WHEN 'TENNIS' THEN 'tennis' ELSE 'pickleball' END;

-- Accounts: CUSTOMER from registration, PROVIDER granted by the admin on approval -------------------------------------
INSERT INTO sporthub.users (id, email, email_normalized, password_hash, display_name, phone, status, email_verified_at)
SELECT pg_temp.seed_uuid(1100 + k), 'chusan' || (k + 5) || '@vesanre.demo', 'chusan' || (k + 5) || '@vesanre.demo',
       '$2a$10$NCvWsqF.yuJJLaoji3iV6eUIc9wATaepnx775kVn6tidNp9wPeDf2', owner_name, '09050001' || lpad(k::text, 2, '0'),
       'ACTIVE', now()
FROM seed9_venue;

INSERT INTO sporthub.user_roles (user_id, role_code, granted_by)
SELECT pg_temp.seed_uuid(1100 + k), 'CUSTOMER', NULL FROM seed9_venue
UNION ALL
SELECT pg_temp.seed_uuid(1100 + k), 'PROVIDER', pg_temp.seed_uuid(1) FROM seed9_venue;

-- Provider side: VERIFIED profile, approved verification, one ACTIVE shop each -------------------------------------
INSERT INTO sporthub.provider_profiles (user_id, legal_name, status, verified_at)
SELECT pg_temp.seed_uuid(1100 + k), 'Hộ kinh doanh ' || shop_name, 'VERIFIED', now() - interval '60 days' FROM seed9_venue;

INSERT INTO sporthub.provider_verifications (id, submitted_by, documents, status, reviewed_by, reviewed_at, provider_user_id,
                                             created_at, updated_at)
SELECT pg_temp.seed_uuid(1400 + k), pg_temp.seed_uuid(1100 + k), '[]'::jsonb, 'APPROVED', pg_temp.seed_uuid(1),
       now() - interval '60 days', pg_temp.seed_uuid(1100 + k), now() - interval '61 days', now() - interval '60 days'
FROM seed9_venue;

INSERT INTO sporthub.shops (id, owner_user_id, slug, name, description, status)
SELECT pg_temp.seed_uuid(1200 + k), pg_temp.seed_uuid(1100 + k), shop_slug, shop_name,
       'Cụm ' || courts || ' sân ' || sport_label || ' tại ' || district || ', ' || city || '.', 'ACTIVE'
FROM seed9_venue;

INSERT INTO sporthub.venues (id, shop_id, slug, name, description, address_line, ward, district, city, province,
                             latitude, longitude, phone, status, deposit_percent)
SELECT pg_temp.seed_uuid(1300 + k), pg_temp.seed_uuid(1200 + k), venue_slug, venue_name,
       courts || ' sân ' || sport_label || ' tại phường ' || ward || ', ' || district || ', có bãi giữ xe và wifi miễn phí.',
       address_line, ward, district, city, city, latitude, longitude, venue_phone, 'ACTIVE', deposit
FROM seed9_venue;

-- Every venue has PARKING and WIFI; football adds SHOWER, racket sports add EQUIPMENT_RENTAL.
INSERT INTO sporthub.venue_amenities (venue_id, amenity_id, details)
SELECT pg_temp.seed_uuid(1300 + sv.k), am.id, NULL
FROM seed9_venue sv
JOIN sporthub.amenities am ON am.code = 'PARKING' OR am.code = 'WIFI'
    OR (am.code = 'SHOWER' AND sv.sport IN ('FOOTBALL_5', 'FOOTBALL_7'))
    OR (am.code = 'EQUIPMENT_RENTAL' AND sv.sport IN ('BADMINTON', 'TENNIS', 'PICKLEBALL'));

-- Courts S1..Sn per venue; identical sport, hours (all 7 days) and price list, as VenueSetupService writes them ------
CREATE TEMP TABLE seed9_court ON COMMIT DROP AS
SELECT sv.k, c AS n, pg_temp.seed_uuid(2000 + sv.k * 10 + c) AS id
FROM seed9_venue sv CROSS JOIN LATERAL generate_series(1, sv.courts) AS c;

INSERT INTO sporthub.courts (id, venue_id, code, name, capacity, booking_step_minutes, min_booking_minutes, max_booking_minutes, status)
SELECT sc.id, pg_temp.seed_uuid(1300 + sc.k), 'S' || sc.n, 'Sân ' || sc.n, 4, 60, 60, 120, 'ACTIVE' FROM seed9_court sc;

INSERT INTO sporthub.court_sports (court_id, sport_id, is_primary)
SELECT sc.id, sp.id, true FROM seed9_court sc
JOIN seed9_venue sv ON sv.k = sc.k JOIN sporthub.sports sp ON sp.code = sv.sport;

INSERT INTO sporthub.court_operating_hours (court_id, weekday, opens_at, closes_at, is_closed)
SELECT sc.id, wd, make_time(sv.open_h, 0, 0), make_time(sv.close_h, 0, 0), false
FROM seed9_court sc JOIN seed9_venue sv ON sv.k = sc.k CROSS JOIN generate_series(0, 6) AS wd;

-- Normal price until 17h, peak 17h-22h (or closing time), normal again after 22h.
INSERT INTO sporthub.court_pricing_rules (id, court_id, weekday, start_minute, end_minute, price_per_hour, currency, is_active)
SELECT md5('seed-rule-' || sc.id || '-' || wd || '-' || r.start_minute)::uuid, sc.id, wd, r.start_minute, r.end_minute, r.price, 'VND', true
FROM seed9_court sc JOIN seed9_venue sv ON sv.k = sc.k CROSS JOIN generate_series(0, 6) AS wd
CROSS JOIN LATERAL (VALUES
    (sv.open_h * 60, 17 * 60, sv.normal_price),
    (17 * 60, LEAST(22, sv.close_h) * 60, sv.peak_price),
    (22 * 60, sv.close_h * 60, sv.normal_price)
) AS r(start_minute, end_minute, price)
WHERE r.start_minute < r.end_minute;

-- Shop wallets ------------------------------------------------------------------------------------------------------
INSERT INTO sporthub.coin_wallets (id, user_id, shop_id, balance)
SELECT pg_temp.seed_uuid(1500 + k), NULL, pg_temp.seed_uuid(1200 + k), 0 FROM seed9_venue;
