CREATE TABLE IF NOT EXISTS users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS posts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title VARCHAR(255),
    content VARCHAR(255),
    status VARCHAR(255) DEFAULT 'DRAFT',
    created_at TIMESTAMP,
    author_id UUID REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comments (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content VARCHAR(255),
    created_at TIMESTAMP,
    post_id UUID REFERENCES posts(id) ON DELETE CASCADE
);
