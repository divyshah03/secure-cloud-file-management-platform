CREATE SEQUENCE IF NOT EXISTS audit_log_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY DEFAULT nextval('audit_log_id_seq'),
    actor_user_id BIGINT,
    actor_email VARCHAR(255),
    action VARCHAR(40) NOT NULL,
    file_id BIGINT,
    file_name_snapshot VARCHAR(255),
    metadata VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_log_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_log_file_id ON audit_log(file_id);
CREATE INDEX idx_audit_log_actor_user_id ON audit_log(actor_user_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);
