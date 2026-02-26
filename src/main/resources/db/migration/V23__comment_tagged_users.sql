-- Tagged/mentioned users in comments (@mentions)
CREATE TABLE IF NOT EXISTS comment_tagged_users (
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (comment_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_comment_tagged_users_comment_id ON comment_tagged_users(comment_id);
CREATE INDEX IF NOT EXISTS idx_comment_tagged_users_user_id ON comment_tagged_users(user_id);
