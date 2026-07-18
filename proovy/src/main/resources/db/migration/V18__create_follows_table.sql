CREATE TABLE follows (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    follower_id  BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (follower_id != following_id)
);

CREATE UNIQUE INDEX uq_follows_follower_following ON follows (follower_id, following_id);
CREATE INDEX idx_follows_following_id ON follows (following_id);
CREATE INDEX idx_follows_follower_id ON follows (follower_id);
