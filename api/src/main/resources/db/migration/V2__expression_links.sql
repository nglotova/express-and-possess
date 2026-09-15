-- Product links are their own field, entered separately from the description, so they
-- are always shown as links. Stored as a JSON array of strings, in the order entered.
alter table expressions add column links jsonb not null default '[]'::jsonb;
