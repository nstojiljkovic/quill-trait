CREATE TABLE page(
    id INT,
    is_root BOOLEAN,
    parent_id INT,
    sorting INT,
    title VARCHAR,
    path VARCHAR,
    sitemap_change_freq VARCHAR,
    sitemap_priority DOUBLE,
    p01 VARCHAR,
    p02 VARCHAR,
    p03 VARCHAR,
    PRIMARY KEY(id, sorting)
) WITH CLUSTERING ORDER BY (sorting ASC);
