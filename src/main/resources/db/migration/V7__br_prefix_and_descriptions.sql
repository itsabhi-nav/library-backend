-- Use BR prefix instead of BRA for member IDs and config examples

UPDATE library_config
SET config_value = 'BR'
WHERE config_key = 'member_id_prefix' AND config_value = 'BRA';

UPDATE library_config
SET description = 'Prefix for new member IDs (e.g. BR → BR001). Only affects future registrations.'
WHERE config_key = 'member_id_prefix';

UPDATE library_config
SET description = 'Number of digits in auto-generated member IDs (e.g. 3 → BR001)'
WHERE config_key = 'member_id_digits';

-- Rename existing BRA### member IDs to BR### (BRA001 → BR001)
UPDATE users
SET member_id = 'BR' || SUBSTRING(member_id FROM 4)
WHERE member_id LIKE 'BRA%' AND LENGTH(member_id) > 3;
