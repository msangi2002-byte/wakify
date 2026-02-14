-- SQL Queries to Change User Role to ADMIN
-- The users table has a role enum: 'VISITOR','USER','BUSINESS','AGENT','ADMIN'

-- Option 1: Change role by user email
UPDATE `users` 
SET `role` = 'ADMIN', `updated_at` = NOW(6)
WHERE `email` = 'user@example.com';

-- Option 2: Change role by user phone number
UPDATE `users` 
SET `role` = 'ADMIN', `updated_at` = NOW(6)
WHERE `phone` = '+255712345678';

-- Option 3: Change role by user ID (binary UUID format)
-- Replace the binary value with the actual user ID
UPDATE `users` 
SET `role` = 'ADMIN', `updated_at` = NOW(6)
WHERE `id` = 0x1a531d9d25a54266a87e10f0ae60e187;

-- Option 4: Change role by user ID (using UNHEX for string UUID)
-- Replace 'your-uuid-here' with the actual UUID string (without dashes)
UPDATE `users` 
SET `role` = 'ADMIN', `updated_at` = NOW(6)
WHERE `id` = UNHEX(REPLACE('your-uuid-here', '-', ''));

-- Option 5: Change role by user name (if unique)
UPDATE `users` 
SET `role` = 'ADMIN', `updated_at` = NOW(6)
WHERE `name` = 'User Name';

-- Example: Change specific user to admin (replace with actual email)
-- UPDATE `users` 
-- SET `role` = 'ADMIN', `updated_at` = NOW(6)
-- WHERE `email` = 'test@wakilfy.com';
