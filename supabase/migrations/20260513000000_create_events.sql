create extension if not exists pgcrypto;

create table if not exists public.events (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    description text,
    location text,
    image_url text,
    owner_id text not null,
    allow_guest boolean not null default false,
    start_date timestamptz,
    end_date timestamptz,
    created_at timestamptz not null default now()
);

alter table if exists public.events
    add column if not exists owner_id text;

alter table if exists public.events
    add column if not exists title text,
    add column if not exists description text,
    add column if not exists location text,
    add column if not exists image_url text,
    add column if not exists allow_guest boolean not null default false,
    add column if not exists start_date timestamptz,
    add column if not exists end_date timestamptz,
    add column if not exists created_at timestamptz not null default now();

do $$
begin
    -- Migrate data from created_by to owner_id if needed
    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public' and table_name = 'events' and column_name = 'created_by'
    ) then
        execute 'update public.events set owner_id = created_by where owner_id is null and created_by is not null';
    end if;

    -- Set owner_id to empty string where it's still null
    execute 'update public.events set owner_id = '''' where owner_id is null';

    -- Add NOT NULL constraint to owner_id if it doesn't have one
    if not exists (
        select 1
        from information_schema.table_constraints
        where table_schema = 'public' and table_name = 'events' and constraint_name = 'events_owner_id_not_null'
    ) then
        execute 'alter table public.events alter column owner_id set not null';
    end if;

    -- Handle other legacy columns
    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public' and table_name = 'events' and column_name = 'allowGuest'
    ) then
        execute 'update public.events set allow_guest = coalesce(allow_guest, "allowGuest") where allow_guest is null';
    end if;

    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public' and table_name = 'events' and column_name = 'allowguest'
    ) then
        execute 'update public.events set allow_guest = coalesce(allow_guest, allowguest) where allow_guest is null';
    end if;
end
$$;

create index if not exists events_owner_id_idx on public.events (owner_id);
create index if not exists events_created_at_idx on public.events (created_at desc);

alter table public.events enable row level security;

drop policy if exists "events_select_all" on public.events;
create policy "events_select_all"
on public.events
for select
using (true);

drop policy if exists "events_insert_own" on public.events;
create policy "events_insert_own"
on public.events
for insert
with check (owner_id = auth.uid()::text);

drop policy if exists "events_update_own" on public.events;
create policy "events_update_own"
on public.events
for update
using (owner_id = auth.uid()::text)
with check (owner_id = auth.uid()::text);

drop policy if exists "events_delete_own" on public.events;
create policy "events_delete_own"
on public.events
for delete
using (owner_id = auth.uid()::text);