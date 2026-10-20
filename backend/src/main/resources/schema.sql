CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS food_posts (
    id TEXT PRIMARY KEY,
    food_name TEXT NOT NULL,
    description TEXT NOT NULL,
    servings INTEGER NOT NULL CHECK (servings BETWEEN 1 AND 1000),
    owner_id TEXT NOT NULL,
    owner_name TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_food_posts_created_at ON food_posts(created_at DESC);
