-- ============================================================================
-- SAWARGI (com.altomedia.sawargi) — Supabase schema (full core feature set)
-- Migrated from the legacy WoWonder v4.1.4 PHP app.
-- Run this in the Supabase Dashboard -> SQL Editor, top to bottom, ONCE.
-- It is idempotent (IF NOT EXISTS / OR REPLACE) so re-running is safe.
-- ============================================================================

-----------------------------------------------------------------------
-- PROFILES  (mirrors auth.users; profile row auto-created on signup)
-----------------------------------------------------------------------
create table if not exists public.profiles (
  id          uuid primary key references auth.users (id) on delete cascade,
  phone       text unique,                       -- primary identity (login = phone)
  email       text,                              -- synthetic <phone>@sawargi.app
  username    text unique,
  full_name   text,
  avatar      text,
  cover       text,
  bio         text,
  gender      text,
  birthday    date,
  address     text,
  country     text,
  verified    boolean not null default false,
  is_admin    boolean not null default false,
  is_pro      boolean not null default false,
  lastseen    timestamptz,
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
alter table public.profiles enable row level security;

create policy "Profiles viewable by all"         on public.profiles for select using (true);
create policy "Users insert own profile"         on public.profiles for insert with check (auth.uid() = id);
create policy "Users update own profile"         on public.profiles for update using (auth.uid() = id);
create policy "Users delete own profile"         on public.profiles for delete using (auth.uid() = id);

-- Auto-create a profile row whenever a new auth user is created.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, phone, email, full_name)
  values (
    new.id,
    new.raw_user_meta_data ->> 'phone',
    new.email,
    coalesce(new.raw_user_meta_data ->> 'full_name', '')
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Bump updated_at automatically
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists profiles_set_updated on public.profiles;
create trigger profiles_set_updated before update on public.profiles
  for each row execute function public.set_updated_at();

-----------------------------------------------------------------------
-- POSTS (feed: status/text, photo, video, link, feeling, shared)
-----------------------------------------------------------------------
create table if not exists public.posts (
  id             bigint generated always as identity primary key,
  user_id        uuid not null references public.profiles (id) on delete cascade,
  text           text,
  media          text,          -- image/video URL  (single media)
  media_type     text default 'image', -- image | video | none
  link           text,
  link_title     text,
  link_image     text,
  feeling        text,          -- "Saya merasa: ..."
  type           text not null default 'status', -- status|photo|video|link|feeling|share
  shared_post_id bigint references public.posts (id) on delete set null,
  post_privacy   smallint not null default 0, -- 0 all | 1 followers | 2 only me
  boosted        boolean not null default false,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now()
);
alter table public.posts enable row level security;

create policy "Posts viewable by all"            on public.posts for select using (true);
create policy "Users insert own post"            on public.posts for insert with check (auth.uid() = user_id);
create policy "Users update own post"            on public.posts for update using (auth.uid() = user_id);
create policy "Users delete own post"            on public.posts for delete using (auth.uid() = user_id);

drop trigger if exists posts_set_updated on public.posts;
create trigger posts_set_updated before update on public.posts
  for each row execute function public.set_updated_at();

-- Count caches driven by triggers
alter table public.posts add column if not exists reactions_count int not null default 0;
alter table public.posts add column if not exists comments_count int not null default 0;
alter table public.posts add column if not exists shares_count  int not null default 0;

-----------------------------------------------------------------------
-- COMMENTS + REPLIES
-----------------------------------------------------------------------
create table if not exists public.comments (
  id         bigint generated always as identity primary key,
  post_id    bigint not null references public.posts (id) on delete cascade,
  user_id    uuid not null references public.profiles (id) on delete cascade,
  text       text not null,
  created_at timestamptz not null default now()
);
alter table public.comments enable row level security;
create policy "Comments viewable by all"  on public.comments for select using (true);
create policy "Users insert own comment"  on public.comments for insert with check (auth.uid() = user_id);
create policy "Users delete own comment"  on public.comments for delete using (auth.uid() = user_id);

create table if not exists public.comment_replies (
  id         bigint generated always as identity primary key,
  comment_id bigint not null references public.comments (id) on delete cascade,
  user_id    uuid not null references public.profiles (id) on delete cascade,
  text       text not null,
  created_at timestamptz not null default now()
);
alter table public.comment_replies enable row level security;
create policy "Replies viewable by all"   on public.comment_replies for select using (true);
create policy "Users insert own reply"    on public.comment_replies for insert with check (auth.uid() = user_id);
create policy "Users delete own reply"    on public.comment_replies for delete using (auth.uid() = user_id);

-----------------------------------------------------------------------
-- REACTIONS  (like/love/haha/wow/sad/angry on posts; likes on comments)
-----------------------------------------------------------------------
create table if not exists public.post_reactions (
  id         bigint generated always as identity primary key,
  user_id    uuid not null references public.profiles (id) on delete cascade,
  post_id    bigint references public.posts (id) on delete cascade,
  comment_id bigint references public.comments (id) on delete cascade,
  type       text not null check (type in ('like','love','haha','wow','sad','angry')),
  created_at timestamptz not null default now(),
  unique (user_id, post_id),
  unique (user_id, comment_id),
  check ( (post_id is not null) <> (comment_id is not null) )
);
alter table public.post_reactions enable row level security;
create policy "Reactions viewable by all"  on public.post_reactions for select using (true);
create policy "Users insert own reaction"  on public.post_reactions for insert with check (auth.uid() = user_id);
create policy "Users delete own reaction"  on public.post_reactions for delete using (auth.uid() = user_id);

-- update posts.reactions_count
create or replace function public.recalc_post_reactions()
returns trigger language plpgsql as $$
begin
  if tg_op in ('INSERT','DELETE') then
    if tg_op = 'INSERT' and new.post_id is not null then
      update public.posts set reactions_count = (select count(*) from public.post_reactions where post_id = new.post_id) where id = new.post_id;
    elsif tg_op = 'DELETE' and old.post_id is not null then
      update public.posts set reactions_count = (select count(*) from public.post_reactions where post_id = old.post_id) where id = old.post_id;
    end if;
  end if;
  return coalesce(new, old);
end;
$$;
drop trigger if exists reactions_count_trg on public.post_reactions;
create trigger reactions_count_trg after insert or delete on public.post_reactions
  for each row execute function public.recalc_post_reactions();

-- update posts.comments_count
create or replace function public.recalc_post_comments()
returns trigger language plpgsql as $$
begin
  if tg_op = 'INSERT' then
    update public.posts set comments_count = (select count(*) from public.comments where post_id = new.post_id) where id = new.post_id;
  elsif tg_op = 'DELETE' then
    update public.posts set comments_count = (select count(*) from public.comments where post_id = old.post_id) where id = old.post_id;
  end if;
  return coalesce(new, old);
end;
$$;
drop trigger if exists comments_count_trg on public.comments;
create trigger comments_count_trg after insert or delete on public.comments
  for each row execute function public.recalc_post_comments();

-----------------------------------------------------------------------
-- FOLLOWERS  (= follows AND friend/follow requests, like Wo_Followers)
-- status: 'pending' => request not yet accepted ; 'accepted' => following/friend
-----------------------------------------------------------------------
create table if not exists public.followers (
  id        bigint generated always as identity primary key,
  follower  uuid not null references public.profiles (id) on delete cascade,
  following uuid not null references public.profiles (id) on delete cascade,
  status    text not null default 'accepted' check (status in ('pending','accepted','blocked')),
  created_at timestamptz not null default now(),
  unique (follower, following),
  check (follower <> following)
);
alter table public.followers enable row level security;
create policy "Followers viewable by all" on public.followers for select using (true);
create policy "Users insert own follow"   on public.followers for insert with check (auth.uid() = follower);
create policy "Users delete own follow"   on public.followers for delete using (auth.uid() = follower or auth.uid() = following);
create policy "Users update own follow"   on public.followers for update using (auth.uid() = follower);

-----------------------------------------------------------------------
-- SAVED POSTS (bookmarks)
-----------------------------------------------------------------------
create table if not exists public.saved_posts (
  id        bigint generated always as identity primary key,
  user_id   uuid not null references public.profiles (id) on delete cascade,
  post_id   bigint not null references public.posts (id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (user_id, post_id)
);
alter table public.saved_posts enable row level security;
create policy "Saved posts own only" on public.saved_posts for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-----------------------------------------------------------------------
-- CONVERSATIONS + MESSAGES (chat / messenger)
-----------------------------------------------------------------------
create table if not exists public.conversations (
  id        bigint generated always as identity primary key,
  user_a    uuid not null references public.profiles (id) on delete cascade,
  user_b    uuid not null references public.profiles (id) on delete cascade,
  last_message_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  unique (user_a, user_b),
  check (user_a < user_b)
);
alter table public.conversations enable row level security;
create policy "Conversation participants only" on public.conversations for select
  using (auth.uid() = user_a or auth.uid() = user_b);

create table if not exists public.messages (
  id        bigint generated always as identity primary key,
  conversation_id bigint not null references public.conversations (id) on delete cascade,
  sender_id uuid not null references public.profiles (id) on delete cascade,
  text      text,
  media     text,
  media_type text default 'none',   -- none|image|video|audio|file
  type      text not null default 'text', -- text|media|sticker
  seen      boolean not null default false,
  created_at timestamptz not null default now()
);
alter table public.messages enable row level security;
create policy "Messages viewable by both" on public.messages for select using (
  exists (select 1 from public.conversations c
          where c.id = messages.conversation_id and (c.user_a = auth.uid() or c.user_b = auth.uid()))
);
create policy "Users send own message" on public.messages for insert with check (auth.uid() = sender_id);

-- Update conversation.last_message_at on new message
create or replace function public.touch_conversation()
returns trigger language plpgsql as $$
begin
  update public.conversations set last_message_at = now()
  where id = new.conversation_id;
  return new;
end;
$$;
drop trigger if exists messages_touch_conv on public.messages;
create trigger messages_touch_conv after insert on public.messages
  for each row execute function public.touch_conversation();

-----------------------------------------------------------------------
-- NOTIFICATIONS
-----------------------------------------------------------------------
create table if not exists public.notifications (
  id         bigint generated always as identity primary key,
  recipient  uuid not null references public.profiles (id) on delete cascade,
  actor      uuid references public.profiles (id) on delete cascade,
  type       text not null,          -- like|comment|follow|share|mention|accepted
  post_id    bigint references public.posts (id) on delete cascade,
  comment_id bigint references public.comments (id) on delete cascade,
  text       text,
  url        text,
  seen       boolean not null default false,
  created_at timestamptz not null default now()
);
alter table public.notifications enable row level security;
create policy "Notifications own only" on public.notifications for all
  using (auth.uid() = recipient) with check (auth.uid() = recipient);

-----------------------------------------------------------------------
-- STORIES + VIEWS
-----------------------------------------------------------------------
create table if not exists public.stories (
  id        bigint generated always as identity primary key,
  user_id   uuid not null references public.profiles (id) on delete cascade,
  media     text not null,
  media_type text not null default 'image',
  created_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '24 hours')
);
alter table public.stories enable row level security;
create policy "Stories viewable by all" on public.stories for select using (true);
create policy "Users insert own story"  on public.stories for insert with check (auth.uid() = user_id);
create policy "Users delete own story"  on public.stories for delete using (auth.uid() = user_id);

create table if not exists public.story_views (
  id       bigint generated always as identity primary key,
  story_id bigint not null references public.stories (id) on delete cascade,
  user_id  uuid not null references public.profiles (id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (story_id, user_id)
);
alter table public.story_views enable row level security;
create policy "Story views viewable by all" on public.story_views for select using (true);
create policy "Users insert own view"       on public.story_views for insert with check (auth.uid() = user_id);

-----------------------------------------------------------------------
-- BLOCKED USERS + RECENT SEARCHES
-----------------------------------------------------------------------
create table if not exists public.blocked_users (
  id        bigint generated always as identity primary key,
  user_id   uuid not null references public.profiles (id) on delete cascade,
  blocked   uuid not null references public.profiles (id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (user_id, blocked)
);
alter table public.blocked_users enable row level security;
create policy "Blocks own only" on public.blocked_users for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

create table if not exists public.recent_searches (
  id        bigint generated always as identity primary key,
  user_id   uuid not null references public.profiles (id) on delete cascade,
  query     text not null,
  created_at timestamptz not null default now()
);
alter table public.recent_searches enable row level security;
create policy "Searches own only" on public.recent_searches for all
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ============================================================================
-- PERMISSIONS (so the publishable/anon key can read; app only reads unless
-- authenticated via the per-table policies above).
-- ============================================================================
grant usage on schema public to anon, authenticated;
grant all on all tables in schema public to authenticated;
grant select on all tables in schema public to anon;
alter default privileges in schema public grant all on tables to authenticated;
alter default privileges in schema public grant select on tables to anon;

-- ============================================================================
-- ADMIN SEED
--   Login Nomor HP : 085813899649
--   Sandi          : Kdsmedia@123
--
-- The app logs in with phone+password, mapped to the synthetic email
-- 085813899649@sawargi.app on the Email provider. You must create the auth
-- user in the Dashboard (Authentication -> Users -> Add user, email
-- 085813899649@sawargi.app, password Kdsmedia@123), then run:
--
--   update public.profiles set is_admin = true, verified = true
--   where id = (select id from auth.users where email = '085813899649@sawargi.app');
-- ============================================================================