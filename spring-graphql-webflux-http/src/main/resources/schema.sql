CREATE TABLE IF NOT EXISTS users
(
    id         UUID DEFAULT uuid_generate_v4(),
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL DEFAULT 'password',
    created_at TIMESTAMP,
    version    INTEGER
);

CREATE TABLE IF NOT EXISTS posts
(
    id         UUID DEFAULT uuid_generate_v4(),
    title      VARCHAR(255),
    content    VARCHAR(255),
    status     VARCHAR(255)       DEFAULT 'DRAFT',
    author_id  UUID,
    created_at TIMESTAMP NOT NULL DEFAULT LOCALTIMESTAMP,
    updated_at TIMESTAMP,
    version    INTEGER
);

CREATE TABLE IF NOT EXISTS comments
(
    id         UUID DEFAULT uuid_generate_v4(),
    content    VARCHAR(255),
    post_id    UUID,
    created_at TIMESTAMP,
    version    INTEGER
);

-- drop foreign key constraint
ALTER TABLE comments
    DROP CONSTRAINT IF EXISTS fk_comments_post_id;
ALTER TABLE posts
    DROP CONSTRAINT IF EXISTS fk_posts_author_id;
-- drop primary key constraint
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS pk_users;
ALTER TABLE posts
    DROP CONSTRAINT IF EXISTS pk_posts;
ALTER TABLE comments
    DROP CONSTRAINT IF EXISTS pk_comments;


-- add primary key constraint
ALTER TABLE users
    ADD CONSTRAINT pk_users PRIMARY KEY (id);
ALTER TABLE posts
    ADD CONSTRAINT pk_posts PRIMARY KEY (id);
ALTER TABLE comments
    ADD CONSTRAINT pk_comments PRIMARY KEY (id);
-- add foreign key constraint
ALTER TABLE posts
    ADD CONSTRAINT fk_posts_author_id FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE;
ALTER TABLE comments
    ADD CONSTRAINT fk_comments_post_id FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE;