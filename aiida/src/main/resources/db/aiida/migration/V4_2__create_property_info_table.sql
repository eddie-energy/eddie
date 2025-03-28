ALTER TABLE public.aiida_record_value
    ADD COLUMN source_key TEXT NULL;

ALTER TABLE public.aiida_record_value
    ADD CONSTRAINT data_tag_or_source_key_required
        CHECK (
            (data_tag IS NOT NULL AND source_key IS NULL)
                OR
            (data_tag IS NULL AND source_key IS NOT NULL)
            )
