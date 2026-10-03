-- The PROVIDER role is now granted when an admin approves the application; every account keeps CUSTOMER.
-- Providers created by the old flow lacked CUSTOMER, and unapproved ones must not hold PROVIDER.
INSERT INTO sporthub.user_roles (user_id, role_code)
SELECT ur.user_id, 'CUSTOMER'
FROM sporthub.user_roles ur
WHERE ur.role_code = 'PROVIDER'
  AND NOT EXISTS (SELECT 1 FROM sporthub.user_roles c WHERE c.user_id = ur.user_id AND c.role_code = 'CUSTOMER');

DELETE FROM sporthub.user_roles ur
WHERE ur.role_code = 'PROVIDER'
  AND NOT EXISTS (SELECT 1 FROM sporthub.provider_profiles p WHERE p.user_id = ur.user_id AND p.status = 'VERIFIED');
