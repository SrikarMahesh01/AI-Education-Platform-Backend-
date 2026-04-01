-- Drop the existing certificates table
DROP TABLE IF EXISTS certificates;

-- Recreate the certificates table with LONGBLOB
CREATE TABLE certificates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    course_name VARCHAR(255),
    issue_date DATETIME,
    certificate_number VARCHAR(255),
    certificate_data LONGBLOB,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
