-- Run once against an existing todoapp database before deploying the category UI.
-- This creates only the category table; todos and its rows are kept intact.
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_name (name)
) DEFAULT CHARACTER SET utf8mb4;

INSERT INTO categories (name) VALUES
    ('デザイン'), ('マーケティング'), ('プログラミング'), ('資格'), ('就職活動')
ON DUPLICATE KEY UPDATE name = name;

-- Include categories used by existing and soft-deleted Todo rows.
INSERT INTO categories (name)
SELECT DISTINCT category FROM todos
WHERE category IS NOT NULL AND TRIM(category) <> ''
ON DUPLICATE KEY UPDATE name = name;
