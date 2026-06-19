-- Meta-approved template: library_admission (English, image header)

INSERT INTO whatsapp_templates (
    template_name,
    template_language,
    template_status,
    template_category,
    template_content,
    header_type,
    header_content,
    footer_text,
    variables,
    org_id
) VALUES (
    'library_admission',
    'en',
    'approved',
    'MARKETING',
    '📚 *Welcome to BR Ambedkar Library!*

Dear {{1}},

We are delighted to welcome you to *BR Ambedkar Library*.

Your membership has been successfully registered. We are committed to providing a peaceful, disciplined, and resourceful environment to support your learning and academic growth.

Thank you for choosing *BR Ambedkar Library*. We wish you a productive and successful learning journey ahead.

📚 *BR Ambedkar Library, Nadipar*
*Unit of Udayan Public School, Japla*',
    'IMAGE',
    'https://res.cloudinary.com/dcahaaigp/image/upload/v1781909500/school_tfd0v6.png',
    'This is an automated message. Please do not reply.',
    '[{"name": "{{1}}", "type": "text", "example": "Rahul Kumar"}]'::jsonb,
    'library'
)
ON CONFLICT (template_name, template_language, org_id) DO UPDATE SET
    template_status = EXCLUDED.template_status,
    template_category = EXCLUDED.template_category,
    template_content = EXCLUDED.template_content,
    header_type = EXCLUDED.header_type,
    header_content = EXCLUDED.header_content,
    footer_text = EXCLUDED.footer_text,
    variables = EXCLUDED.variables,
    updated_at = NOW();
