-- Reference DDL (Hibernate ddl-auto=update generates this automatically at startup;
-- this script is kept for documentation / manual SQLite setup if preferred over H2).

CREATE TABLE IF NOT EXISTS profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255),
    bio VARCHAR(2000),
    followers BIGINT,
    following BIGINT,
    posts_count BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    instagram_post_id VARCHAR(255) NOT NULL,
    profile_id BIGINT NOT NULL,
    caption VARCHAR(4000),
    likes BIGINT,
    comments BIGINT,
    post_date TIMESTAMP,
    post_url VARCHAR(500),
    FOREIGN KEY (profile_id) REFERENCES profiles(id)
);
