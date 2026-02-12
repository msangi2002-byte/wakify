-- Make agent_id optional for business requests (users with account complete via USSD; no agent needed)
ALTER TABLE business_requests MODIFY COLUMN agent_id BINARY(16) NULL;
