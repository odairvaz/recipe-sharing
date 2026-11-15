-- Review comments are validated up to 1000 characters, and recipe descriptions are free text,
-- but both columns were created as VARCHAR(255).
ALTER TABLE reviews ALTER COLUMN comment TYPE VARCHAR(1000);
ALTER TABLE recipe ALTER COLUMN description TYPE TEXT;
