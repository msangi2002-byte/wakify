-- Add DRAFT and PENDING_CONFIRMATION to orders.status enum (inquiry flow)
-- Without this, "Create order" from accepted inquiry fails: Data truncated for column 'status'

ALTER TABLE `orders`
  MODIFY COLUMN `status` enum(
    'DRAFT',
    'PENDING_CONFIRMATION',
    'PENDING',
    'CONFIRMED',
    'PROCESSING',
    'SHIPPED',
    'DELIVERED',
    'COMPLETED',
    'CANCELLED',
    'REFUNDED'
  ) DEFAULT NULL;
