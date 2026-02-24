-- CTA link for MESSAGES / TRAFFIC objectives: when user clicks sponsored post, open this URL
ALTER TABLE promotions ADD COLUMN cta_link VARCHAR(2048) NULL;
