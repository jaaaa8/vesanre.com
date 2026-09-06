CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE SCHEMA IF NOT EXISTS sporthub;

CREATE TABLE sporthub.users (
    id uuid PRIMARY KEY,
    email varchar(320) NOT NULL,
    email_normalized varchar(320) NOT NULL,
    password_hash varchar(255) NOT NULL,
    display_name varchar(120) NOT NULL,
    phone varchar(32),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email_normalized UNIQUE (email_normalized),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED'))
);

CREATE UNIQUE INDEX uq_users_phone_not_null ON sporthub.users (phone) WHERE phone IS NOT NULL;
CREATE INDEX ix_users_status ON sporthub.users (status);

CREATE TABLE sporthub.roles (
    code varchar(30) PRIMARY KEY,
    name varchar(80) NOT NULL,
    description text
);

CREATE TABLE sporthub.user_roles (
    user_id uuid NOT NULL,
    role_code varchar(30) NOT NULL,
    granted_by uuid,
    granted_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES sporthub.users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_code) REFERENCES sporthub.roles (code) ON DELETE RESTRICT,
    CONSTRAINT fk_user_roles_granted_by FOREIGN KEY (granted_by) REFERENCES sporthub.users (id) ON DELETE SET NULL
);

CREATE INDEX ix_user_roles_role_user ON sporthub.user_roles (role_code, user_id);
CREATE INDEX ix_user_roles_granted_by ON sporthub.user_roles (granted_by);

INSERT INTO sporthub.roles (code, name, description) VALUES
    ('CUSTOMER', 'Customer', 'Customer who searches and books courts'),
    ('PROVIDER', 'Provider', 'Venue provider'),
    ('ADMIN', 'Administrator', 'Platform administrator');

CREATE TABLE sporthub.provider_profiles (
    user_id uuid PRIMARY KEY,
    legal_name varchar(200) NOT NULL,
    tax_id varchar(50),
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    verified_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_provider_profiles_user FOREIGN KEY (user_id) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_provider_profiles_status CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED', 'SUSPENDED')),
    CONSTRAINT ck_provider_profiles_verified_at CHECK (status <> 'VERIFIED' OR verified_at IS NOT NULL)
);

CREATE UNIQUE INDEX uq_provider_profiles_tax_id_not_null ON sporthub.provider_profiles (tax_id) WHERE tax_id IS NOT NULL;
CREATE INDEX ix_provider_profiles_status ON sporthub.provider_profiles (status);

CREATE TABLE sporthub.shops (
    id uuid PRIMARY KEY,
    owner_user_id uuid NOT NULL,
    slug varchar(120) NOT NULL,
    name varchar(160) NOT NULL,
    description text,
    logo_storage_key varchar(500),
    default_cancellation_policy jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(30) NOT NULL DEFAULT 'DRAFT',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_shops_owner UNIQUE (owner_user_id),
    CONSTRAINT uq_shops_slug UNIQUE (slug),
    CONSTRAINT fk_shops_owner FOREIGN KEY (owner_user_id) REFERENCES sporthub.provider_profiles (user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_shops_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'ACTIVE', 'REJECTED', 'SUSPENDED'))
);

CREATE INDEX ix_shops_status ON sporthub.shops (status);

CREATE TABLE sporthub.shop_members (
    shop_id uuid NOT NULL,
    user_id uuid NOT NULL,
    member_role varchar(20) NOT NULL,
    joined_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at timestamptz,
    PRIMARY KEY (shop_id, user_id),
    CONSTRAINT fk_shop_members_shop FOREIGN KEY (shop_id) REFERENCES sporthub.shops (id) ON DELETE CASCADE,
    CONSTRAINT fk_shop_members_user FOREIGN KEY (user_id) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_shop_members_role CHECK (member_role IN ('MANAGER', 'STAFF'))
);

CREATE INDEX ix_shop_members_user ON sporthub.shop_members (user_id);

CREATE TABLE sporthub.provider_verifications (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL,
    submitted_by uuid NOT NULL,
    documents jsonb NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by uuid,
    reviewed_at timestamptz,
    rejection_reason text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_provider_verifications_shop FOREIGN KEY (shop_id) REFERENCES sporthub.shops (id) ON DELETE RESTRICT,
    CONSTRAINT fk_provider_verifications_submitted_by FOREIGN KEY (submitted_by) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_provider_verifications_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_provider_verifications_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT ck_provider_verifications_review_fields CHECK (
        (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL AND rejection_reason IS NULL)
        OR (status = 'APPROVED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NULL)
        OR (status = 'REJECTED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_provider_verifications_pending_shop
    ON sporthub.provider_verifications (shop_id) WHERE status = 'PENDING';
CREATE INDEX ix_provider_verifications_submitted_by ON sporthub.provider_verifications (submitted_by);
CREATE INDEX ix_provider_verifications_reviewed_by ON sporthub.provider_verifications (reviewed_by);

CREATE TABLE sporthub.sports (
    id uuid PRIMARY KEY,
    code varchar(50) NOT NULL,
    name varchar(100) NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_sports_code UNIQUE (code),
    CONSTRAINT uq_sports_name UNIQUE (name)
);

CREATE TABLE sporthub.amenities (
    id uuid PRIMARY KEY,
    code varchar(50) NOT NULL,
    name varchar(100) NOT NULL,
    allowed_scope varchar(10) NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_amenities_code UNIQUE (code),
    CONSTRAINT uq_amenities_name UNIQUE (name),
    CONSTRAINT ck_amenities_scope CHECK (allowed_scope IN ('VENUE', 'COURT', 'BOTH'))
);

CREATE TABLE sporthub.venues (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL,
    slug varchar(120) NOT NULL,
    name varchar(160) NOT NULL,
    description text,
    address_line varchar(255) NOT NULL,
    ward varchar(120),
    district varchar(120) NOT NULL,
    city varchar(120) NOT NULL,
    province varchar(120),
    postal_code varchar(20),
    latitude numeric(9,6),
    longitude numeric(9,6),
    timezone varchar(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    phone varchar(32),
    status varchar(30) NOT NULL DEFAULT 'DRAFT',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_venues_shop_slug UNIQUE (shop_id, slug),
    CONSTRAINT fk_venues_shop FOREIGN KEY (shop_id) REFERENCES sporthub.shops (id) ON DELETE RESTRICT,
    CONSTRAINT ck_venues_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'ACTIVE', 'REJECTED', 'SUSPENDED')),
    CONSTRAINT ck_venues_coordinates CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
    )
);

CREATE INDEX ix_venues_shop_status ON sporthub.venues (shop_id, status);
CREATE INDEX ix_venues_location_status ON sporthub.venues (city, district, status);

CREATE TABLE sporthub.courts (
    id uuid PRIMARY KEY,
    venue_id uuid NOT NULL,
    code varchar(50) NOT NULL,
    name varchar(120) NOT NULL,
    description text,
    capacity integer NOT NULL,
    booking_step_minutes integer NOT NULL,
    min_booking_minutes integer NOT NULL,
    max_booking_minutes integer NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_courts_venue_code UNIQUE (venue_id, code),
    CONSTRAINT fk_courts_venue FOREIGN KEY (venue_id) REFERENCES sporthub.venues (id) ON DELETE RESTRICT,
    CONSTRAINT ck_courts_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_courts_capacity CHECK (capacity > 0),
    CONSTRAINT ck_courts_booking_minutes CHECK (
        booking_step_minutes > 0
        AND min_booking_minutes > 0
        AND max_booking_minutes >= min_booking_minutes
        AND min_booking_minutes % booking_step_minutes = 0
        AND max_booking_minutes % booking_step_minutes = 0
    )
);

CREATE INDEX ix_courts_venue_status ON sporthub.courts (venue_id, status);

CREATE TABLE sporthub.court_sports (
    court_id uuid NOT NULL,
    sport_id uuid NOT NULL,
    is_primary boolean NOT NULL DEFAULT false,
    PRIMARY KEY (court_id, sport_id),
    CONSTRAINT fk_court_sports_court FOREIGN KEY (court_id) REFERENCES sporthub.courts (id) ON DELETE CASCADE,
    CONSTRAINT fk_court_sports_sport FOREIGN KEY (sport_id) REFERENCES sporthub.sports (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_court_sports_primary ON sporthub.court_sports (court_id) WHERE is_primary = true;
CREATE INDEX ix_court_sports_sport ON sporthub.court_sports (sport_id);

CREATE TABLE sporthub.venue_amenities (
    venue_id uuid NOT NULL,
    amenity_id uuid NOT NULL,
    details varchar(255),
    PRIMARY KEY (venue_id, amenity_id),
    CONSTRAINT fk_venue_amenities_venue FOREIGN KEY (venue_id) REFERENCES sporthub.venues (id) ON DELETE CASCADE,
    CONSTRAINT fk_venue_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES sporthub.amenities (id) ON DELETE RESTRICT
);

CREATE INDEX ix_venue_amenities_amenity ON sporthub.venue_amenities (amenity_id);

CREATE TABLE sporthub.court_amenities (
    court_id uuid NOT NULL,
    amenity_id uuid NOT NULL,
    details varchar(255),
    PRIMARY KEY (court_id, amenity_id),
    CONSTRAINT fk_court_amenities_court FOREIGN KEY (court_id) REFERENCES sporthub.courts (id) ON DELETE CASCADE,
    CONSTRAINT fk_court_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES sporthub.amenities (id) ON DELETE RESTRICT
);

CREATE INDEX ix_court_amenities_amenity ON sporthub.court_amenities (amenity_id);

CREATE TABLE sporthub.venue_images (
    id uuid PRIMARY KEY,
    venue_id uuid NOT NULL,
    storage_key varchar(500) NOT NULL,
    alt_text varchar(255),
    sort_order integer NOT NULL,
    is_cover boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_venue_images_storage_key UNIQUE (storage_key),
    CONSTRAINT uq_venue_images_venue_sort UNIQUE (venue_id, sort_order),
    CONSTRAINT fk_venue_images_venue FOREIGN KEY (venue_id) REFERENCES sporthub.venues (id) ON DELETE CASCADE,
    CONSTRAINT ck_venue_images_sort_order CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX uq_venue_images_cover ON sporthub.venue_images (venue_id) WHERE is_cover = true;

CREATE TABLE sporthub.court_images (
    id uuid PRIMARY KEY,
    court_id uuid NOT NULL,
    storage_key varchar(500) NOT NULL,
    alt_text varchar(255),
    sort_order integer NOT NULL,
    is_cover boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_court_images_storage_key UNIQUE (storage_key),
    CONSTRAINT uq_court_images_court_sort UNIQUE (court_id, sort_order),
    CONSTRAINT fk_court_images_court FOREIGN KEY (court_id) REFERENCES sporthub.courts (id) ON DELETE CASCADE,
    CONSTRAINT ck_court_images_sort_order CHECK (sort_order >= 0)
);

CREATE UNIQUE INDEX uq_court_images_cover ON sporthub.court_images (court_id) WHERE is_cover = true;

CREATE TABLE sporthub.court_operating_hours (
    court_id uuid NOT NULL,
    weekday smallint NOT NULL,
    opens_at time without time zone,
    closes_at time without time zone,
    is_closed boolean NOT NULL DEFAULT false,
    PRIMARY KEY (court_id, weekday),
    CONSTRAINT fk_court_operating_hours_court FOREIGN KEY (court_id) REFERENCES sporthub.courts (id) ON DELETE CASCADE,
    CONSTRAINT ck_court_operating_hours_weekday CHECK (weekday BETWEEN 0 AND 6),
    CONSTRAINT ck_court_operating_hours_window CHECK (
        (is_closed = true AND opens_at IS NULL AND closes_at IS NULL)
        OR (is_closed = false AND opens_at IS NOT NULL AND closes_at IS NOT NULL AND opens_at < closes_at)
    )
);

CREATE TABLE sporthub.court_pricing_rules (
    id uuid PRIMARY KEY,
    court_id uuid NOT NULL,
    weekday smallint NOT NULL,
    start_minute integer NOT NULL,
    end_minute integer NOT NULL,
    minute_range int4range GENERATED ALWAYS AS (int4range(start_minute, end_minute, '[)')) STORED,
    price_per_hour numeric(12,2) NOT NULL,
    currency varchar(3) NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_court_pricing_rules_court FOREIGN KEY (court_id) REFERENCES sporthub.courts (id) ON DELETE CASCADE,
    CONSTRAINT ck_court_pricing_rules_weekday CHECK (weekday BETWEEN 0 AND 6),
    CONSTRAINT ck_court_pricing_rules_minutes CHECK (start_minute >= 0 AND end_minute <= 1440 AND start_minute < end_minute),
    CONSTRAINT ck_court_pricing_rules_price CHECK (price_per_hour > 0),
    CONSTRAINT ck_court_pricing_rules_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ex_court_pricing_rules_active_overlap EXCLUDE USING gist (
        court_id WITH =,
        weekday WITH =,
        minute_range WITH &&
    ) WHERE (is_active = true)
);

CREATE INDEX ix_court_pricing_rules_lookup ON sporthub.court_pricing_rules (court_id, weekday, is_active);

CREATE TABLE sporthub.court_schedule_blocks (
    id uuid PRIMARY KEY,
    court_id uuid NOT NULL,
    block_kind varchar(20) NOT NULL,
    state varchar(20) NOT NULL DEFAULT 'ACTIVE',
    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    occupied_period tstzrange GENERATED ALWAYS AS (tstzrange(start_at, end_at, '[)')) STORED,
    created_by uuid NOT NULL,
    reason text,
    released_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_court_schedule_blocks_court FOREIGN KEY (court_id) REFERENCES sporthub.courts (id) ON DELETE RESTRICT,
    CONSTRAINT fk_court_schedule_blocks_created_by FOREIGN KEY (created_by) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_court_schedule_blocks_kind CHECK (block_kind IN ('BOOKING', 'MAINTENANCE', 'CLOSURE')),
    CONSTRAINT ck_court_schedule_blocks_state CHECK (state IN ('ACTIVE', 'RELEASED')),
    CONSTRAINT ck_court_schedule_blocks_period CHECK (start_at < end_at),
    CONSTRAINT ck_court_schedule_blocks_release CHECK (
        (state = 'ACTIVE' AND released_at IS NULL)
        OR (state = 'RELEASED' AND released_at IS NOT NULL)
    ),
    CONSTRAINT ex_court_schedule_blocks_active_overlap EXCLUDE USING gist (
        court_id WITH =,
        occupied_period WITH &&
    ) WHERE (state = 'ACTIVE')
);

CREATE INDEX ix_court_schedule_blocks_court_state_start ON sporthub.court_schedule_blocks (court_id, state, start_at);
CREATE INDEX ix_court_schedule_blocks_kind_state ON sporthub.court_schedule_blocks (block_kind, state);
CREATE INDEX ix_court_schedule_blocks_created_by ON sporthub.court_schedule_blocks (created_by);

CREATE TABLE sporthub.bookings (
    id uuid PRIMARY KEY,
    booking_code varchar(30) NOT NULL,
    customer_id uuid NOT NULL,
    schedule_block_id uuid NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'HELD',
    hold_expires_at timestamptz,
    shop_name_snapshot varchar(160) NOT NULL,
    venue_name_snapshot varchar(160) NOT NULL,
    court_name_snapshot varchar(120) NOT NULL,
    unit_price_snapshot numeric(12,2) NOT NULL,
    subtotal_amount numeric(12,2) NOT NULL,
    deposit_amount numeric(12,2) NOT NULL,
    currency varchar(3) NOT NULL,
    price_breakdown_snapshot jsonb NOT NULL,
    cancellation_policy_snapshot jsonb NOT NULL,
    cancellation_source varchar(20),
    cancelled_by uuid,
    cancellation_reason text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at timestamptz,
    cancelled_at timestamptz,
    completed_at timestamptz,
    CONSTRAINT uq_bookings_code UNIQUE (booking_code),
    CONSTRAINT uq_bookings_schedule_block UNIQUE (schedule_block_id),
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_schedule_block FOREIGN KEY (schedule_block_id) REFERENCES sporthub.court_schedule_blocks (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_bookings_status CHECK (status IN ('HELD', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'COMPLETED')),
    CONSTRAINT ck_bookings_amounts CHECK (
        unit_price_snapshot >= 0 AND subtotal_amount >= 0
        AND deposit_amount >= 0 AND deposit_amount <= subtotal_amount
    ),
    CONSTRAINT ck_bookings_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_bookings_hold_expiry CHECK (status <> 'HELD' OR hold_expires_at IS NOT NULL),
    CONSTRAINT ck_bookings_cancellation CHECK (
        (status = 'CANCELLED' AND cancellation_source IN ('CUSTOMER', 'PROVIDER', 'ADMIN') AND cancelled_at IS NOT NULL)
        OR (status <> 'CANCELLED' AND cancellation_source IS NULL AND cancelled_at IS NULL)
    )
);

CREATE INDEX ix_bookings_customer_created ON sporthub.bookings (customer_id, created_at DESC);
CREATE INDEX ix_bookings_status_created ON sporthub.bookings (status, created_at);
CREATE INDEX ix_bookings_cancelled_by ON sporthub.bookings (cancelled_by);
CREATE INDEX ix_bookings_held_expiry ON sporthub.bookings (hold_expires_at) WHERE status = 'HELD';

CREATE TABLE sporthub.booking_status_history (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    booking_id uuid NOT NULL,
    from_status varchar(20),
    to_status varchar(20) NOT NULL,
    actor_type varchar(20) NOT NULL,
    actor_user_id uuid,
    reason_code varchar(80),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_status_history_booking FOREIGN KEY (booking_id) REFERENCES sporthub.bookings (id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_status_history_actor FOREIGN KEY (actor_user_id) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_booking_status_history_from CHECK (from_status IS NULL OR from_status IN ('HELD', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'COMPLETED')),
    CONSTRAINT ck_booking_status_history_to CHECK (to_status IN ('HELD', 'CONFIRMED', 'CANCELLED', 'EXPIRED', 'COMPLETED')),
    CONSTRAINT ck_booking_status_history_actor_type CHECK (actor_type IN ('CUSTOMER', 'PROVIDER', 'ADMIN', 'SYSTEM', 'PAYMENT')),
    CONSTRAINT ck_booking_status_history_changed CHECK (from_status IS NULL OR from_status <> to_status)
);

CREATE INDEX ix_booking_status_history_booking_created ON sporthub.booking_status_history (booking_id, created_at);
CREATE INDEX ix_booking_status_history_actor_created ON sporthub.booking_status_history (actor_user_id, created_at);

CREATE TABLE sporthub.payment_intents (
    id uuid PRIMARY KEY,
    booking_id uuid NOT NULL,
    provider_code varchar(50) NOT NULL,
    idempotency_key varchar(120) NOT NULL,
    provider_intent_id varchar(160),
    amount numeric(12,2) NOT NULL,
    currency varchar(3) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'CREATED',
    expires_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_intents_idempotency UNIQUE (provider_code, idempotency_key),
    CONSTRAINT fk_payment_intents_booking FOREIGN KEY (booking_id) REFERENCES sporthub.bookings (id) ON DELETE RESTRICT,
    CONSTRAINT ck_payment_intents_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_intents_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_payment_intents_status CHECK (status IN ('CREATED', 'PENDING', 'SUCCEEDED', 'FAILED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uq_payment_intents_provider_id
    ON sporthub.payment_intents (provider_code, provider_intent_id) WHERE provider_intent_id IS NOT NULL;
CREATE UNIQUE INDEX uq_payment_intents_succeeded_booking
    ON sporthub.payment_intents (booking_id) WHERE status = 'SUCCEEDED';
CREATE INDEX ix_payment_intents_booking_status ON sporthub.payment_intents (booking_id, status);

CREATE TABLE sporthub.payment_webhook_events (
    id uuid PRIMARY KEY,
    provider_code varchar(50) NOT NULL,
    provider_event_id varchar(160) NOT NULL,
    event_type varchar(100) NOT NULL,
    payload jsonb NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'RECEIVED',
    received_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at timestamptz,
    error_message text,
    CONSTRAINT uq_webhook_events_provider_event UNIQUE (provider_code, provider_event_id),
    CONSTRAINT ck_webhook_events_status CHECK (status IN ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED')),
    CONSTRAINT ck_webhook_events_processed CHECK (
        (status = 'RECEIVED' AND processed_at IS NULL)
        OR (status <> 'RECEIVED' AND processed_at IS NOT NULL)
    )
);

CREATE INDEX ix_webhook_events_status_received ON sporthub.payment_webhook_events (status, received_at);

CREATE TABLE sporthub.payment_transactions (
    id uuid PRIMARY KEY,
    payment_intent_id uuid NOT NULL,
    webhook_event_id uuid,
    provider_code varchar(50) NOT NULL,
    provider_transaction_id varchar(160) NOT NULL,
    amount numeric(12,2) NOT NULL,
    currency varchar(3) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    occurred_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payment_transactions_provider_tx UNIQUE (provider_code, provider_transaction_id),
    CONSTRAINT fk_payment_transactions_intent FOREIGN KEY (payment_intent_id) REFERENCES sporthub.payment_intents (id) ON DELETE RESTRICT,
    CONSTRAINT fk_payment_transactions_webhook FOREIGN KEY (webhook_event_id) REFERENCES sporthub.payment_webhook_events (id) ON DELETE RESTRICT,
    CONSTRAINT ck_payment_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_transactions_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_payment_transactions_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED'))
);

CREATE UNIQUE INDEX uq_payment_transactions_webhook
    ON sporthub.payment_transactions (webhook_event_id) WHERE webhook_event_id IS NOT NULL;
CREATE INDEX ix_payment_transactions_intent_status ON sporthub.payment_transactions (payment_intent_id, status);

CREATE TABLE sporthub.refund_requests (
    id uuid PRIMARY KEY,
    booking_id uuid NOT NULL,
    payment_transaction_id uuid NOT NULL,
    trigger_webhook_event_id uuid,
    confirmation_webhook_event_id uuid,
    provider_code varchar(50) NOT NULL,
    idempotency_key varchar(120) NOT NULL,
    provider_refund_id varchar(160),
    amount numeric(12,2) NOT NULL,
    currency varchar(3) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    attempt_count integer NOT NULL DEFAULT 0,
    next_retry_at timestamptz,
    last_error text,
    requested_by uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at timestamptz,
    CONSTRAINT uq_refund_requests_transaction UNIQUE (payment_transaction_id),
    CONSTRAINT uq_refund_requests_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_refund_requests_booking FOREIGN KEY (booking_id) REFERENCES sporthub.bookings (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refund_requests_transaction FOREIGN KEY (payment_transaction_id) REFERENCES sporthub.payment_transactions (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refund_requests_trigger_webhook FOREIGN KEY (trigger_webhook_event_id) REFERENCES sporthub.payment_webhook_events (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refund_requests_confirmation_webhook FOREIGN KEY (confirmation_webhook_event_id) REFERENCES sporthub.payment_webhook_events (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refund_requests_requested_by FOREIGN KEY (requested_by) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_refund_requests_amount CHECK (amount > 0),
    CONSTRAINT ck_refund_requests_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_refund_requests_status CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY_PENDING', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT ck_refund_requests_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_refund_requests_completion CHECK (status <> 'SUCCEEDED' OR completed_at IS NOT NULL)
);

CREATE UNIQUE INDEX uq_refund_requests_trigger_webhook
    ON sporthub.refund_requests (trigger_webhook_event_id) WHERE trigger_webhook_event_id IS NOT NULL;
CREATE UNIQUE INDEX uq_refund_requests_confirmation_webhook
    ON sporthub.refund_requests (confirmation_webhook_event_id) WHERE confirmation_webhook_event_id IS NOT NULL;
CREATE UNIQUE INDEX uq_refund_requests_provider_id
    ON sporthub.refund_requests (provider_code, provider_refund_id) WHERE provider_refund_id IS NOT NULL;
CREATE INDEX ix_refund_requests_status_retry ON sporthub.refund_requests (status, next_retry_at);
CREATE INDEX ix_refund_requests_booking ON sporthub.refund_requests (booking_id);
CREATE INDEX ix_refund_requests_requested_by ON sporthub.refund_requests (requested_by);

CREATE TABLE sporthub.reviews (
    id uuid PRIMARY KEY,
    booking_id uuid NOT NULL,
    customer_id uuid NOT NULL,
    rating smallint NOT NULL,
    comment text,
    provider_reply text,
    replied_by uuid,
    replied_at timestamptz,
    status varchar(20) NOT NULL DEFAULT 'PUBLISHED',
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reviews_booking UNIQUE (booking_id),
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES sporthub.bookings (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_replied_by FOREIGN KEY (replied_by) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_status CHECK (status IN ('PUBLISHED', 'HIDDEN')),
    CONSTRAINT ck_reviews_reply CHECK (
        (provider_reply IS NULL AND replied_by IS NULL AND replied_at IS NULL)
        OR (provider_reply IS NOT NULL AND replied_by IS NOT NULL AND replied_at IS NOT NULL)
    )
);

CREATE INDEX ix_reviews_customer_created ON sporthub.reviews (customer_id, created_at);
CREATE INDEX ix_reviews_status_created ON sporthub.reviews (status, created_at);
CREATE INDEX ix_reviews_replied_by ON sporthub.reviews (replied_by);

CREATE TABLE sporthub.notifications (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    booking_id uuid,
    notification_type varchar(80) NOT NULL,
    title varchar(160) NOT NULL,
    body text NOT NULL,
    dedupe_key varchar(160) NOT NULL,
    read_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notifications_dedupe_key UNIQUE (dedupe_key),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_notifications_booking FOREIGN KEY (booking_id) REFERENCES sporthub.bookings (id) ON DELETE RESTRICT
);

CREATE INDEX ix_notifications_user_created ON sporthub.notifications (user_id, created_at DESC);
CREATE INDEX ix_notifications_booking ON sporthub.notifications (booking_id);
CREATE INDEX ix_notifications_unread ON sporthub.notifications (user_id, created_at) WHERE read_at IS NULL;

CREATE TABLE sporthub.outbox_events (
    id uuid PRIMARY KEY,
    event_key varchar(160) NOT NULL,
    aggregate_type varchar(80) NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type varchar(100) NOT NULL,
    payload jsonb NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    attempt_count integer NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by varchar(120),
    locked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at timestamptz,
    CONSTRAINT uq_outbox_events_event_key UNIQUE (event_key),
    CONSTRAINT ck_outbox_events_status CHECK (status IN ('PENDING', 'PROCESSING', 'RETRY_PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_events_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_outbox_events_lock CHECK (
        (status = 'PROCESSING' AND locked_by IS NOT NULL AND locked_at IS NOT NULL)
        OR (status <> 'PROCESSING' AND locked_by IS NULL AND locked_at IS NULL)
    ),
    CONSTRAINT ck_outbox_events_published CHECK (status <> 'PUBLISHED' OR published_at IS NOT NULL)
);

CREATE INDEX ix_outbox_events_status_attempt ON sporthub.outbox_events (status, next_attempt_at);

CREATE TABLE sporthub.moderation_actions (
    id uuid PRIMARY KEY,
    actor_admin_id uuid NOT NULL,
    target_type varchar(20) NOT NULL,
    shop_id uuid,
    venue_id uuid,
    action_type varchar(20) NOT NULL,
    reason text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_moderation_actions_actor FOREIGN KEY (actor_admin_id) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_moderation_actions_shop FOREIGN KEY (shop_id) REFERENCES sporthub.shops (id) ON DELETE RESTRICT,
    CONSTRAINT fk_moderation_actions_venue FOREIGN KEY (venue_id) REFERENCES sporthub.venues (id) ON DELETE RESTRICT,
    CONSTRAINT ck_moderation_actions_target_type CHECK (target_type IN ('SHOP', 'VENUE')),
    CONSTRAINT ck_moderation_actions_action_type CHECK (action_type IN ('APPROVE', 'REJECT', 'SUSPEND', 'REACTIVATE')),
    CONSTRAINT ck_moderation_actions_target CHECK (
        (target_type = 'SHOP' AND shop_id IS NOT NULL AND venue_id IS NULL)
        OR (target_type = 'VENUE' AND venue_id IS NOT NULL AND shop_id IS NULL)
    )
);

CREATE INDEX ix_moderation_actions_actor_created ON sporthub.moderation_actions (actor_admin_id, created_at);
CREATE INDEX ix_moderation_actions_shop_created ON sporthub.moderation_actions (shop_id, created_at);
CREATE INDEX ix_moderation_actions_venue_created ON sporthub.moderation_actions (venue_id, created_at);

CREATE TABLE sporthub.audit_logs (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    actor_user_id uuid,
    action varchar(100) NOT NULL,
    entity_type varchar(100) NOT NULL,
    entity_id uuid NOT NULL,
    before_data jsonb,
    after_data jsonb,
    request_id varchar(120),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES sporthub.users (id) ON DELETE SET NULL
);

CREATE INDEX ix_audit_logs_entity_created ON sporthub.audit_logs (entity_type, entity_id, created_at);
CREATE INDEX ix_audit_logs_actor_created ON sporthub.audit_logs (actor_user_id, created_at);
