CREATE TABLE IF NOT EXISTS pfps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    picture_data LONGBLOB,
    picture_type VARCHAR(50),
    content_type VARCHAR(100),
    file_name VARCHAR(255),
    uploaded_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
