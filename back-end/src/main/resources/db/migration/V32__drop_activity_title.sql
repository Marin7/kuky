-- Activities no longer have a title of their own: wherever one needs a name it is shown
-- as "Actividad – página N" (the page title lives in unit_page_titles).
ALTER TABLE activities DROP COLUMN title;
