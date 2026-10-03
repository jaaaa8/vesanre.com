-- Provider approval state lives in provider_profiles.status; shop approval state lives in shops.status.
-- A verification therefore belongs to the provider, not to the shop.
ALTER TABLE sporthub.provider_verifications
    ADD COLUMN provider_user_id uuid;

UPDATE sporthub.provider_verifications pv
SET provider_user_id = s.owner_user_id
FROM sporthub.shops s
WHERE s.id = pv.shop_id;

ALTER TABLE sporthub.provider_verifications
    ALTER COLUMN provider_user_id SET NOT NULL,
    ADD CONSTRAINT fk_provider_verifications_provider
        FOREIGN KEY (provider_user_id) REFERENCES sporthub.provider_profiles (user_id) ON DELETE RESTRICT;

DROP INDEX sporthub.uq_provider_verifications_pending_shop;

ALTER TABLE sporthub.provider_verifications
    DROP CONSTRAINT fk_provider_verifications_shop,
    DROP COLUMN shop_id;

CREATE UNIQUE INDEX uq_provider_verifications_pending_provider
    ON sporthub.provider_verifications (provider_user_id) WHERE status = 'PENDING';
CREATE INDEX ix_provider_verifications_provider ON sporthub.provider_verifications (provider_user_id);
