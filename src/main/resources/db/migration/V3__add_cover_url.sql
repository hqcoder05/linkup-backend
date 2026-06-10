DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name='users' AND column_name='cover_url'
    ) THEN
        ALTER TABLE users ADD COLUMN cover_url VARCHAR(500);
    END IF;
END
$$;
