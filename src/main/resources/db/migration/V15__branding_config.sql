-- Branding text shown on login and inside the app (admin-editable in System Config)

INSERT INTO library_config (config_key, config_value, description)
VALUES (
    'school_affiliation',
    'Run by Udayan Public School',
    'School affiliation line under the library name on login and in the app sidebar'
)
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO library_config (config_key, config_value, description)
VALUES (
    'developer_credit',
    'Designed and Developed by Abhinav',
    'Developer credit shown on login and in the app footer after sign-in'
)
ON CONFLICT (config_key) DO NOTHING;
