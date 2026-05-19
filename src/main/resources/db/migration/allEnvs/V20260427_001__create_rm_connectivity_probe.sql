CREATE TABLE rm_connectivity_probe (
    probe_id SMALLINT PRIMARY KEY,
    description VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO rm_connectivity_probe (probe_id, description)
VALUES (1, 'Initial RM connectivity probe');
