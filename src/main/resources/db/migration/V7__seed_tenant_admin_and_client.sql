INSERT INTO users (tenant_id, email, password_hash, role, first_name, last_name)
VALUES (NULL,
        'tenant@mail.ru',
        '$2a$10$j9XiI2ZTCEAmTyulmLvYke8dxNQt7gEitCO0LQ6zvKDuBwEUjcPR2',
        'TENANT_ADMIN',
        'tenant',
        'user');

INSERT INTO users (tenant_id, email, password_hash, role, first_name, last_name)
VALUES (NULL,
        'client@mail.ru',
        '$2a$10$1eKGWrN0pf506mff5F598OSOITGNkq77GgU88PCvwoLM6YkKocktu',
        'CLIENT',
        'client',
        'client');
