-- Post-login onboarding flow (Facebook-like).
-- New users: onboardingCompleted = false → must complete profile setup.
-- Existing users: DEFAULT true so they skip onboarding.
ALTER TABLE users ADD COLUMN onboarding_completed BOOLEAN DEFAULT TRUE NOT NULL;
