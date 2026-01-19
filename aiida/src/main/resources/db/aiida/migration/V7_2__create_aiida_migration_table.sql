CREATE TABLE IF NOT EXISTS aiida_migration
(
    installed_rank SERIAL PRIMARY KEY,
    migration_key        TEXT UNIQUE,
    description    TEXT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);