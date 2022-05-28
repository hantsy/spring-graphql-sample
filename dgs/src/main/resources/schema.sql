CREATE TABLE IF NOT EXISTS users
(
    id         BIGSERIAL,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS posts
(
    id         BIGSERIAL,
    title      VARCHAR(255),
    content    VARCHAR(255),
    status     VARCHAR(255)       DEFAULT 'DRAFT',
    author_id  BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS comments
(
    id         BIGSERIAL,
    content    VARCHAR(255),
    post_id    BIGINT NOT NULL
);

-- drop foreign key constraint
ALTER TABLE comments
    DROP CONSTRAINT IF EXISTS fk_comments_post_id;
ALTER TABLE posts
    DROP CONSTRAINT IF EXISTS fk_posts_author_id;
-- drop primary key constraint
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS fk_users;
ALTER TABLE posts
    DROP CONSTRAINT IF EXISTS fk_posts;
ALTER TABLE comments
    DROP CONSTRAINT IF EXISTS fk_comments;


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