CREATE TABLE user (id bigint not null, is_ldap_user boolean not null, password_hash varchar(255) not null, username varchar(255) not null unique, primary key (id));
