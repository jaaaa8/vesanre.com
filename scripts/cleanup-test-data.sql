-- Run: psql -h localhost -U postgres -d vesanre_test -f scripts/cleanup-test-data.sql
-- Deletes integration-test junk: every user with an @example.com email and everything they own or touched.
-- Seed accounts (@vesanre.demo), @test.sporthub.local and real accounts are kept. Safe to run again.

\set ON_ERROR_STOP on
BEGIN;

CREATE TEMP TABLE j_users ON COMMIT DROP AS
SELECT id FROM sporthub.users WHERE email LIKE '%@example.com';
CREATE TEMP TABLE j_shops ON COMMIT DROP AS
SELECT id FROM sporthub.shops WHERE owner_user_id IN (SELECT id FROM j_users);
CREATE TEMP TABLE j_venues ON COMMIT DROP AS
SELECT id FROM sporthub.venues WHERE shop_id IN (SELECT id FROM j_shops);
CREATE TEMP TABLE j_courts ON COMMIT DROP AS
SELECT id FROM sporthub.courts WHERE venue_id IN (SELECT id FROM j_venues);
CREATE TEMP TABLE j_blocks ON COMMIT DROP AS
SELECT id FROM sporthub.court_schedule_blocks
WHERE court_id IN (SELECT id FROM j_courts) OR created_by IN (SELECT id FROM j_users);
CREATE TEMP TABLE j_bookings ON COMMIT DROP AS
SELECT id FROM sporthub.bookings
WHERE customer_id IN (SELECT id FROM j_users) OR cancelled_by IN (SELECT id FROM j_users)
   OR schedule_block_id IN (SELECT id FROM j_blocks);
-- The block of a junk booking is junk too, even when someone else created it.
INSERT INTO j_blocks
SELECT schedule_block_id FROM sporthub.bookings WHERE id IN (SELECT id FROM j_bookings)
EXCEPT SELECT id FROM j_blocks;
CREATE TEMP TABLE j_wallets ON COMMIT DROP AS
SELECT id FROM sporthub.coin_wallets WHERE user_id IN (SELECT id FROM j_users) OR shop_id IN (SELECT id FROM j_shops);
CREATE TEMP TABLE j_withdrawals ON COMMIT DROP AS
SELECT id FROM sporthub.shop_withdrawals
WHERE shop_id IN (SELECT id FROM j_shops) OR requested_by IN (SELECT id FROM j_users) OR reviewed_by IN (SELECT id FROM j_users);
CREATE TEMP TABLE j_topups ON COMMIT DROP AS
SELECT id FROM sporthub.coin_top_ups WHERE wallet_id IN (SELECT id FROM j_wallets);

-- Children first, in foreign-key order.
DELETE FROM sporthub.notifications
WHERE user_id IN (SELECT id FROM j_users) OR booking_id IN (SELECT id FROM j_bookings);
DELETE FROM sporthub.reviews
WHERE booking_id IN (SELECT id FROM j_bookings) OR customer_id IN (SELECT id FROM j_users) OR replied_by IN (SELECT id FROM j_users);
DELETE FROM sporthub.coin_ledger_entries
WHERE wallet_id IN (SELECT id FROM j_wallets) OR booking_id IN (SELECT id FROM j_bookings)
   OR withdrawal_id IN (SELECT id FROM j_withdrawals) OR top_up_id IN (SELECT id FROM j_topups);
DELETE FROM sporthub.booking_status_history
WHERE booking_id IN (SELECT id FROM j_bookings) OR actor_user_id IN (SELECT id FROM j_users);
DELETE FROM sporthub.bookings WHERE id IN (SELECT id FROM j_bookings);
DELETE FROM sporthub.court_schedule_blocks WHERE id IN (SELECT id FROM j_blocks);
DELETE FROM sporthub.coin_top_ups WHERE id IN (SELECT id FROM j_topups);
DELETE FROM sporthub.shop_withdrawals WHERE id IN (SELECT id FROM j_withdrawals);
DELETE FROM sporthub.coin_wallets WHERE id IN (SELECT id FROM j_wallets);
DELETE FROM sporthub.moderation_actions
WHERE shop_id IN (SELECT id FROM j_shops) OR venue_id IN (SELECT id FROM j_venues) OR actor_admin_id IN (SELECT id FROM j_users);
DELETE FROM sporthub.catalog_change_requests
WHERE submitted_by IN (SELECT id FROM j_users) OR reviewed_by IN (SELECT id FROM j_users)
   OR target_id IN (SELECT id FROM j_shops UNION SELECT id FROM j_venues);
DELETE FROM sporthub.courts WHERE id IN (SELECT id FROM j_courts); -- hours, prices, sports, images, amenities cascade
DELETE FROM sporthub.venues WHERE id IN (SELECT id FROM j_venues);
DELETE FROM sporthub.shop_members WHERE user_id IN (SELECT id FROM j_users);
DELETE FROM sporthub.shops WHERE id IN (SELECT id FROM j_shops);
DELETE FROM sporthub.provider_verifications
WHERE provider_user_id IN (SELECT id FROM j_users) OR submitted_by IN (SELECT id FROM j_users) OR reviewed_by IN (SELECT id FROM j_users);
DELETE FROM sporthub.provider_profiles WHERE user_id IN (SELECT id FROM j_users);
DELETE FROM sporthub.users WHERE id IN (SELECT id FROM j_users); -- user_roles cascade, audit_logs set null

COMMIT;

SELECT count(*) AS example_users_left FROM sporthub.users WHERE email LIKE '%@example.com';
