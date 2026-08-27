-- =============================================================
-- SAWARGI (com.altomedia.sawargi) — Supabase schema
-- Run this in the Supabase Dashboard -> SQL Editor
-- BEFORE using the Android app (tables don't exist yet in the project).
-- =============================================================

-- 1) PROFILES: mirrors a row for every auth user
create table if not exists public.profiles (
  id          uuid primary key references auth.users (id) on delete cascade,
  username    text,
  full_name   text,
  phone       text unique,
  email       text,
  avatar      text,
  bio         text,
  is_admin    boolean not null default false,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
alter table public.profiles enable row level security;

create policy "Profiles are viewable by everyone"
  on public.profiles for select using (true);
create policy "Users can insert their own profile"
  on public.profiles for insert with check (auth.uid() = id);
create policy "Users can update their own profile"
  on public.profiles for update using (auth.uid() = id);

-- Auto-create profile on sign-up
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, full_name, phone, email)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', ''),
    new.phone,
    new.email
  )
  on conflict (id) do nothing;
  return new;
end;
$$ language plpgsql security definer set search_path = public;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- 2) POSTS
create table if not exists public.posts (
  id         bigint generated always as identity primary key,
  user_id    uuid not null references public.profiles (id) on delete cascade,
  text       text,
  image      text,
  type       text not null default 'text',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
alter table public.posts enable row level security;

create policy "Posts are viewable by everyone"
  on public.posts for select using (true);
create policy "Users can insert their own posts"
  on public.posts for insert with check (auth.uid() = user_id);
create policy "Users can update their own posts"
  on public.posts for update using (auth.uid() = user_id);
create policy "Users can delete their own posts"
  on public.posts for delete using (auth.uid() = user_id);

-- 3) COMMENTS
create table if not exists public.comments (
  id         bigint generated always as identity primary key,
  post_id    bigint not null references public.posts (id) on delete cascade,
  user_id    uuid not null references public.profiles (id) on delete cascade,
  text       text not null,
  created_at timestamptz not null default now()
);
alter table public.comments enable row level security;

create policy "Comments are viewable by everyone"
  on public.comments for select using (true);
create policy "Users can insert their own comments"
  on public.comments for insert with check (auth.uid() = user_id);
create policy "Users can delete their own comments"
  on public.comments for delete using (auth.uid() = user_id);

-- =============================================================
-- ADMIN LOGIN (phone + password)
--   Nomor / phone : 085813899649
--   Sandi / pwd   : Kdsmedia@123
--
-- The Android app logs in via Auth.signInWith(Phone){ phone; password }.
-- So you MUST:
--   1) Enable  Authentication -> Providers -> Phone  in the Dashboard.
--   2) Create the admin auth user in
--        Authentication -> Users  ->  Add user
--      (phone 085813899649, password Kdsmedia@123).
--   3) Mark that profile as admin:
--        update public.profiles set is_admin = true
--        where id = (select id from auth.users where phone = '085813899649');
-- =============================================================

-- Grant usage so the publishable/anon key can read the tables:
grant usage on schema public to anon, authenticated;
grant all on table public.profiles, public.posts, public.comments
  to anon, authenticated, service_role;
grant usage on all sequences in schema public to anon, authenticated;