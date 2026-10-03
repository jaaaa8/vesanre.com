CREATE TABLE sporthub.catalog_change_requests (
    id uuid PRIMARY KEY,
    target_type varchar(10) NOT NULL,
    target_id uuid NOT NULL,
    proposed jsonb NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    submitted_by uuid NOT NULL,
    reviewed_by uuid,
    reviewed_at timestamptz,
    rejection_reason text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- target_id has no FK: approved shops/venues are never hard-deleted.
    CONSTRAINT fk_catalog_change_requests_submitted_by FOREIGN KEY (submitted_by) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_catalog_change_requests_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_catalog_change_requests_target_type CHECK (target_type IN ('SHOP', 'VENUE')),
    CONSTRAINT ck_catalog_change_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT ck_catalog_change_requests_review_fields CHECK (
        (status IN ('PENDING', 'CANCELLED') AND reviewed_by IS NULL AND reviewed_at IS NULL AND rejection_reason IS NULL)
        OR (status = 'APPROVED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NULL)
        OR (status = 'REJECTED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_catalog_change_requests_pending
    ON sporthub.catalog_change_requests (target_type, target_id) WHERE status = 'PENDING';
CREATE INDEX ix_catalog_change_requests_submitted_by ON sporthub.catalog_change_requests (submitted_by);
CREATE INDEX ix_catalog_change_requests_reviewed_by ON sporthub.catalog_change_requests (reviewed_by);

-- Shops of already verified providers go live.
UPDATE sporthub.shops SET status = 'ACTIVE'
WHERE owner_user_id IN (SELECT user_id FROM sporthub.provider_profiles WHERE status = 'VERIFIED');

INSERT INTO sporthub.sports (id, code, name) VALUES
    (gen_random_uuid(), 'FOOTBALL_5', 'Bóng đá 5'),
    (gen_random_uuid(), 'FOOTBALL_7', 'Bóng đá 7'),
    (gen_random_uuid(), 'BADMINTON', 'Cầu lông'),
    (gen_random_uuid(), 'TENNIS', 'Tennis'),
    (gen_random_uuid(), 'PICKLEBALL', 'Pickleball'),
    (gen_random_uuid(), 'BASKETBALL', 'Bóng rổ'),
    (gen_random_uuid(), 'VOLLEYBALL', 'Bóng chuyền')
ON CONFLICT DO NOTHING;

INSERT INTO sporthub.amenities (id, code, name, allowed_scope) VALUES
    (gen_random_uuid(), 'PARKING', 'Bãi giữ xe', 'VENUE'),
    (gen_random_uuid(), 'LOCKER_ROOM', 'Phòng thay đồ', 'VENUE'),
    (gen_random_uuid(), 'SHOWER', 'Vòi sen', 'VENUE'),
    (gen_random_uuid(), 'WIFI', 'Wifi', 'VENUE'),
    (gen_random_uuid(), 'LIGHTING', 'Đèn chiếu sáng', 'COURT'),
    (gen_random_uuid(), 'EQUIPMENT_RENTAL', 'Cho thuê dụng cụ', 'BOTH')
ON CONFLICT DO NOTHING;
