-- Face Video: initial Supabase schema
-- Apply through Supabase migrations. Do not expose a service_role key to Android.

create extension if not exists pgcrypto;

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    username text unique,
    display_name text not null default '',
    avatar_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint profiles_username_length check (
        username is null or char_length(username) between 3 and 24
    )
);

create table if not exists public.chats (
    id uuid primary key default gen_random_uuid(),
    title text,
    is_group boolean not null default false,
    created_by uuid not null references auth.users(id) on delete restrict,
    last_message_preview text not null default '',
    last_message_at timestamptz,
    created_at timestamptz not null default now(),
    constraint chats_title_length check (title is null or char_length(title) <= 80)
);

create table if not exists public.chat_members (
    chat_id uuid not null references public.chats(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    role text not null default 'member' check (role in ('owner', 'member')),
    joined_at timestamptz not null default now(),
    primary key (chat_id, user_id)
);

create table if not exists public.messages (
    id uuid primary key default gen_random_uuid(),
    chat_id uuid not null references public.chats(id) on delete cascade,
    sender_id uuid not null default auth.uid() references auth.users(id) on delete restrict,
    body text not null,
    created_at timestamptz not null default now(),
    constraint messages_body_length check (
        char_length(body) between 1 and 4000
    )
);

create index if not exists idx_chat_members_user on public.chat_members(user_id, joined_at desc);
create index if not exists idx_messages_chat_created on public.messages(chat_id, created_at asc);
create index if not exists idx_profiles_username on public.profiles(username);

-- Used by RLS policies to avoid recursive policy evaluation on chat_members.
create or replace function public.is_chat_member(
    p_chat_id uuid,
    p_user_id uuid default auth.uid()
)
returns boolean
language sql
stable
security definer
set search_path = public, pg_temp
as $$
    select exists (
        select 1
        from public.chat_members cm
        where cm.chat_id = p_chat_id
          and cm.user_id = p_user_id
    );
$$;

revoke all on function public.is_chat_member(uuid, uuid) from public;
grant execute on function public.is_chat_member(uuid, uuid) to authenticated;

-- Create a chat and all member rows atomically.
create or replace function public.create_chat(
    p_title text,
    p_is_group boolean,
    p_member_ids uuid[]
)
returns uuid
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    v_chat_id uuid;
    v_members uuid[];
    v_count integer;
begin
    if auth.uid() is null then
        raise exception 'Authentication required';
    end if;

    select array_agg(distinct member_id)
      into v_members
      from unnest(coalesce(p_member_ids, array[]::uuid[]) || array[auth.uid()]) as member_id;

    v_count := coalesce(array_length(v_members, 1), 0);

    if v_count < 2 or v_count > 50 then
        raise exception 'A chat must have between 2 and 50 members';
    end if;

    if not p_is_group and v_count <> 2 then
        raise exception 'A direct chat must have exactly 2 members';
    end if;

    insert into public.chats (title, is_group, created_by)
    values (
        case when p_is_group then nullif(left(trim(coalesce(p_title, '')), 80), '') else null end,
        p_is_group,
        auth.uid()
    )
    returning id into v_chat_id;

    insert into public.chat_members (chat_id, user_id, role)
    select v_chat_id, member_id,
           case when member_id = auth.uid() then 'owner' else 'member' end
    from unnest(v_members) as member_id;

    return v_chat_id;
end;
$$;

revoke all on function public.create_chat(text, boolean, uuid[]) from public;
grant execute on function public.create_chat(text, boolean, uuid[]) to authenticated;

-- Ensure a public, non-sensitive profile is created for each new Auth user.
create or replace function public.handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    insert into public.profiles (id, display_name)
    values (
        new.id,
        coalesce(new.raw_user_meta_data ->> 'display_name', '')
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created_face_video on auth.users;
create trigger on_auth_user_created_face_video
after insert on auth.users
for each row execute procedure public.handle_new_auth_user();

-- Update the conversation preview whenever a new message arrives.
create or replace function public.update_chat_after_message()
returns trigger
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
    update public.chats
       set last_message_preview = left(new.body, 160),
           last_message_at = new.created_at
     where id = new.chat_id;
    return new;
end;
$$;

drop trigger if exists after_face_video_message_insert on public.messages;
create trigger after_face_video_message_insert
after insert on public.messages
for each row execute procedure public.update_chat_after_message();

-- A view using invoker permissions preserves base-table RLS.
create or replace view public.chat_summaries
with (security_invoker = true)
as
select
    c.id,
    c.title,
    c.is_group,
    c.last_message_preview,
    c.last_message_at,
    c.created_at
from public.chats c
join public.chat_members cm on cm.chat_id = c.id
where cm.user_id = auth.uid();

alter table public.profiles enable row level security;
alter table public.chats enable row level security;
alter table public.chat_members enable row level security;
alter table public.messages enable row level security;

drop policy if exists "Authenticated users can view public profiles" on public.profiles;
create policy "Authenticated users can view public profiles"
on public.profiles for select to authenticated
using (auth.uid() is not null);

drop policy if exists "Users can insert their own profile" on public.profiles;
create policy "Users can insert their own profile"
on public.profiles for insert to authenticated
with check (id = auth.uid());

drop policy if exists "Users can update their own profile" on public.profiles;
create policy "Users can update their own profile"
on public.profiles for update to authenticated
using (id = auth.uid())
with check (id = auth.uid());

drop policy if exists "Members can view their chats" on public.chats;
create policy "Members can view their chats"
on public.chats for select to authenticated
using (public.is_chat_member(id));

drop policy if exists "Creators can view chat members" on public.chat_members;
create policy "Creators can view chat members"
on public.chat_members for select to authenticated
using (public.is_chat_member(chat_id));

drop policy if exists "Members can read messages" on public.messages;
create policy "Members can read messages"
on public.messages for select to authenticated
using (public.is_chat_member(chat_id));

drop policy if exists "Members can send messages as themselves" on public.messages;
create policy "Members can send messages as themselves"
on public.messages for insert to authenticated
with check (
    sender_id = auth.uid()
    and public.is_chat_member(chat_id)
);

grant select, insert, update on public.profiles to authenticated;
grant select on public.chats to authenticated;
grant select on public.chat_members to authenticated;
grant select, insert on public.messages to authenticated;
grant select on public.chat_summaries to authenticated;

-- Enable postgres_changes for the message stream, idempotently.
do $$
begin
    if not exists (
        select 1
        from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'messages'
    ) then
        alter publication supabase_realtime add table public.messages;
    end if;
end;
$$;
