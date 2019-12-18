CREATE TABLE hibernate_sequence (next_val bigint);
INSERT INTO hibernate_sequence (next_val) VALUES (0);
CREATE TABLE user (id bigint not null, is_ldap_user boolean not null, password_hash varchar(255) not null, username varchar(255) not null unique, primary key (id));
