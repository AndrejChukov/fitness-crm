-- Super admin: admin@fitnesscrm.local / Admin123!
-- BCrypt hash generated with strength 10
INSERT INTO users (tenant_id, email, password_hash, role, first_name, last_name)
VALUES (
    NULL,
    'admin@fitnesscrm.local',
    '$2a$10$s.tvf13oflkelfMcN2tcgO4WdMM3b9lz3HI./RztKrlEsNbZ/UWhy',
    'SUPER_ADMIN',
    'Super',
    'Admin'
);
