create table if not exists persons (
  id bigserial primary key,
  nickname varchar(255) not null unique,
  password varchar(255) not null,
  time_account_id bigint,
  role varchar(255) not null
);