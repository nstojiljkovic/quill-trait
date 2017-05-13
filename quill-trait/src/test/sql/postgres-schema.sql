CREATE TABLE page (
  id SERIAL,
  is_root BOOL DEFAULT FALSE,
  parent_id INT DEFAULT NULL,
  sorting INT DEFAULT NULL,
  title VARCHAR(255),
  path VARCHAR(255),
  sitemap_change_freq VARCHAR(255),
  sitemap_priority NUMERIC(3, 2),
  p01 VARCHAR(255) DEFAULT '',
  p02 VARCHAR(255) DEFAULT NULL,
  p03 VARCHAR(255) DEFAULT NULL
);