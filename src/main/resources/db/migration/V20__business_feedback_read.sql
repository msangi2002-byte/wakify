-- Add read tracking for business feedback (owner can mark as read / read all)
ALTER TABLE business_feedback
  ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN read_at TIMESTAMP NULL;
