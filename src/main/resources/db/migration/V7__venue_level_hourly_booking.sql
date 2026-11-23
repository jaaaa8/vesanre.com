-- Venue-level hourly booking: deposit % lives on the venue, courts are whole-hour only.
ALTER TABLE sporthub.venues ADD COLUMN deposit_percent smallint NOT NULL DEFAULT 0;
ALTER TABLE sporthub.venues ADD CONSTRAINT ck_venues_deposit_percent CHECK (deposit_percent BETWEEN 0 AND 100);

ALTER TABLE sporthub.bookings DROP CONSTRAINT ck_bookings_deposit_coins;
ALTER TABLE sporthub.bookings ADD CONSTRAINT ck_bookings_deposit_coins
    CHECK (deposit_coins >= 0 AND deposit_amount = deposit_coins * 1000);

ALTER TABLE sporthub.courts ADD CONSTRAINT ck_courts_hourly
    CHECK (booking_step_minutes = 60 AND min_booking_minutes = 60 AND max_booking_minutes = 120);

ALTER TABLE sporthub.court_operating_hours ADD CONSTRAINT ck_court_operating_hours_whole_hour CHECK (
    is_closed OR (date_part('minute', opens_at) = 0 AND date_part('second', opens_at) = 0
                  AND date_part('minute', closes_at) = 0 AND date_part('second', closes_at) = 0));

ALTER TABLE sporthub.court_pricing_rules ADD CONSTRAINT ck_court_pricing_rules_whole_hour
    CHECK (start_minute % 60 = 0 AND end_minute % 60 = 0);
