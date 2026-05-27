create extension if not exists pgcrypto;

create table if not exists public.posts (
    id bigint primary key,
    author_id text not null,
    author text not null,
    avatar text not null default '👤',
    profile_image_url text not null default '',
    time text not null default 'now',
    content text not null,
    timestamp bigint not null,
    likes integer not null default 0,
    comments integer not null default 0,
    shares integer not null default 0,
    visibility text not null default 'public',
    allow_comments boolean not null default true,
    is_verified boolean not null default false,
    is_pinned boolean not null default false,
    hidden_from_profile boolean not null default false,
    feeling text,
    location text,
    tags text[] not null default '{}'::text[],
    tagged_people text[] not null default '{}'::text[],
    feeling_emoji text,
    media_urls text[] not null default '{}'::text[],
    media_types text[] not null default '{}'::text[],
    media_emojis text[] not null default '{}'::text[],
    created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
    updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

alter table if exists public.posts
    add column if not exists id bigint,
    add column if not exists author_id text,
    add column if not exists author text,
    add column if not exists avatar text default '👤',
    add column if not exists profile_image_url text default '',
    add column if not exists time text default 'now',
    add column if not exists content text,
    add column if not exists timestamp bigint,
    add column if not exists likes integer not null default 0,
    add column if not exists comments integer not null default 0,
    add column if not exists shares integer not null default 0,
    add column if not exists visibility text default 'public',
    add column if not exists allow_comments boolean not null default true,
    add column if not exists is_verified boolean not null default false,
    add column if not exists is_pinned boolean not null default false,
    add column if not exists hidden_from_profile boolean not null default false,
    add column if not exists feeling text,
    add column if not exists location text,
    add column if not exists tags text[] not null default '{}'::text[],
    add column if not exists tagged_people text[] not null default '{}'::text[],
    add column if not exists feeling_emoji text,
    add column if not exists media_urls text[] not null default '{}'::text[],
    add column if not exists media_types text[] not null default '{}'::text[],
    add column if not exists media_emojis text[] not null default '{}'::text[],
    add column if not exists created_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
    add column if not exists updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint;

create index if not exists posts_author_id_idx on public.posts (author_id);
create index if not exists posts_timestamp_idx on public.posts (timestamp desc);
create index if not exists posts_created_at_idx on public.posts (created_at desc);

alter table public.posts enable row level security;

do $$
begin
    if not exists (
        select 1
        from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'posts'
    ) then
        execute 'alter publication supabase_realtime add table public.posts';
    end if;
end
$$;

drop policy if exists "posts_select_all" on public.posts;
create policy "posts_select_all"
on public.posts
for select
using (true);

drop policy if exists "posts_insert_all" on public.posts;
create policy "posts_insert_all"
on public.posts
for insert
with check (true);

drop policy if exists "posts_update_all" on public.posts;
create policy "posts_update_all"
on public.posts
for update
using (true)
with check (true);

drop policy if exists "posts_delete_all" on public.posts;
create policy "posts_delete_all"
on public.posts
for delete
using (true);
