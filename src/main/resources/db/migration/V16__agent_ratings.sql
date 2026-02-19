-- Agent ratings: business owners rate the agent who activated their business (1-5, optional comment)
CREATE TABLE IF NOT EXISTS agent_ratings (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    rater_user_id VARCHAR(36) NOT NULL,
    agent_id VARCHAR(36) NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP,
    CONSTRAINT fk_agent_rating_rater FOREIGN KEY (rater_user_id) REFERENCES users(id),
    CONSTRAINT fk_agent_rating_agent FOREIGN KEY (agent_id) REFERENCES agents(id),
    CONSTRAINT uk_agent_rating_user_agent UNIQUE (rater_user_id, agent_id)
);
