CREATE TABLE event_publication (
    id UUID PRIMARY KEY,
    publication_date TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    listener_id VARCHAR(255) NOT NULL,
    serialized_event TEXT NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    completion_date TIMESTAMP(6) WITH TIME ZONE,
    last_resubmission_date TIMESTAMP(6) WITH TIME ZONE,
    completion_attempts INTEGER NOT NULL,
    status VARCHAR(255)
);
