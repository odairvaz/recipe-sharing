DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users'
        AND column_name = 'enabled'
    ) THEN
ALTER TABLE users
    ADD COLUMN enabled BOOLEAN DEFAULT FALSE;
END IF;
END$$;
