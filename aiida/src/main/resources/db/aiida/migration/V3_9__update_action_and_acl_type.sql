UPDATE data_source
SET action = 'ALL'
WHERE action = 'SUBSCRIBE';
ALTER TABLE data_source
    ALTER COLUMN action SET DEFAULT 'ALL';