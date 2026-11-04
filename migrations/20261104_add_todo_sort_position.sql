-- Run once on an existing todoapp database before deploying the reorder feature.
-- Existing rows are untouched; NULL means a newly created Todo appears first.
ALTER TABLE todos ADD COLUMN sort_position BIGINT NULL;
