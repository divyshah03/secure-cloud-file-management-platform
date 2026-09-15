CREATE SEQUENCE IF NOT EXISTS file_permission_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE file_permissions (
    id BIGINT PRIMARY KEY DEFAULT nextval('file_permission_id_seq'),
    file_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('EDITOR', 'VIEWER')),
    granted_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_permissions_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_permissions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_permissions_granted_by FOREIGN KEY (granted_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_file_permissions_file_user UNIQUE (file_id, user_id)
);

CREATE INDEX idx_file_permissions_file_id ON file_permissions(file_id);
CREATE INDEX idx_file_permissions_user_id ON file_permissions(user_id);

CREATE SEQUENCE IF NOT EXISTS file_share_link_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE file_share_links (
    id BIGINT PRIMARY KEY DEFAULT nextval('file_share_link_id_seq'),
    file_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('EDITOR', 'VIEWER')),
    created_by BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_share_links_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_share_links_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_file_share_links_file_id ON file_share_links(file_id);
CREATE INDEX idx_file_share_links_token ON file_share_links(token);
