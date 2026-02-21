-- Add composite index for better query performance on permission lookups with status
CREATE INDEX idx_permission_event_permission_id_status ON de_eta.permission_event (permission_id, status);

-- Add index for finding stale permission requests (requests that haven't been updated)
CREATE INDEX idx_permission_event_stale ON de_eta.permission_event (status, event_created) 
WHERE status IN ('REQUESTED', 'PENDING_CONSENT');

-- Add index for data source connection lookups
CREATE INDEX idx_permission_event_connection_id ON de_eta.permission_event (data_source_connection_id);
