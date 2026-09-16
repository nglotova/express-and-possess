-- Links live in the description again (family feedback, 2026-09-15): the page finds every
-- web address in the text and shows it as a link. Move whatever V2's separate field held
-- back into the description, skipping addresses the text already contains.
update expressions e
   set description = e.description || coalesce(
       (select E'\n' || string_agg(l.link, E'\n' order by l.position)
          from jsonb_array_elements_text(e.links) with ordinality as l(link, position)
         where strpos(e.description, l.link) = 0),
       '')
 where jsonb_array_length(e.links) > 0;

alter table expressions drop column links;

-- When the creator adds no picture, one is taken from the first link in the description.
-- This is the address it was taken from, or tried and failed; null means the creator
-- uploaded the picture, which is then never replaced.
alter table expressions add column picture_link varchar(2000);
