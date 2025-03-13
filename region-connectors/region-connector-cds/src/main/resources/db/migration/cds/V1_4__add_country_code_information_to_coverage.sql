
ALTER TABLE cds.coverage
    ADD COLUMN country_code varchar(2) NOT NULL DEFAULT 'us';

ALTER TABLE cds.coverage
    ALTER COLUMN country_code DROP DEFAULT;