CREATE TABLE image
(
    id           uuid PRIMARY KEY                  DEFAULT gen_random_uuid(),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    data         bytea                    NOT NULL,
    content_type TEXT                     NOT NULL
);

ALTER TABLE data_source
    ADD COLUMN icon     VARCHAR(255) NOT NULL DEFAULT 'ELECTRICITY',
    ADD COLUMN image_id uuid         NULL REFERENCES image (id);

