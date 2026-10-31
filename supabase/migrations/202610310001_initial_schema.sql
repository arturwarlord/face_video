-- Face Video schema, isolated from other apps in this Supabase project.
-- All app-owned public objects use the face_video_ prefix.
-- Android uses only the publishable key; database RLS is the security boundary.

create schema if not exists face_video_private;
revoke all on schema face_video_private from public, anon;
grant usage on schema face_video_private to authenticated;

create table if not exists public.face_video_profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    username text not null unique,
    display_name text not null default '',
    avatar_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint face_video_profiles_username_length
        check (char_length(username) between 3 and 24),
    constraint face_video_profiles_username_format
        check (username ~ '^[a-z0-9_]{3,24}$')
);

create table if not exists public.face_video_chats (
    id uuid primary key default gen_random_uuid(),
    title text,
    is_group boolean not null default false,
    created_by uuid not null references auth.users(id) on delete restrict,
    last_message_preview text not null default '',
    last_message_at timestamptz,
    created_at timestamptz not null default now(),
    constraint face_video_chats_title_length
        check (title is null or char_length(title) <= 80)
);

create table if not exists public.face_video_chat_members (
    chat_id uuid not null references public.face_video_chats(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    role text not null default 'member' check (role in ('owner', 'member')),
    joined_at timestamptz not null default now(),
    primary key (chat_id, user_id)
);

create table if not exists public.face_video_messages (
    id uuid primary key default gen_random_uuid(),
    chat_id uuid not null references public.face_video_chats(id) on delete cascade,
    sender_id uuid not null default auth.uid() references auth.users(id) on delete restrict,
    body text not null,
    created_at timestamptz not null default now(),
    constraint face_video_messages_body_length
        check (char_length(body) between 1 and 4000)
);

create index if not exists idx_face_video_members_user
    on public.face_video_chat_members(user_id, joined_at desc);
create index if not exists idx_face_video_messages_chat_created
    on public.face_video_messages(chat_id, created_at asc);
create index if not exists idx_face_video_profiles_username
    on public.face_video_profiles(username);

-- These security-definer helpers live outside the exposed Data API schema.
-- They only perform the exact membership/creator checks required by RLS.
create or replace function face_video_private.face_video_is_chat_member(p_chat_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.face_video_chat_members cm
        where cm.chat_id = p_chat_id
          and cm.user_id = (select auth.uid())
    );
$$;

create or replace function face_video_private.face_video_is_chat_creator(p_chat_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.face_video_chats c
        where c.id = p_chat_id
          and c.created_by = (select auth.uid())
    );
$$;

revoke all on function face_video_private.face_video_is_chat_member(uuid) from public, anon;
revoke all on function face_video_private.face_video_is_chat_creator(uuid) from public, anon;
grant execute on function face_video_private.face_video_is_chat_member(uuid) to authenticated;
grant execute on function face_video_private.face_video_is_chat_creator(uuid) to authenticated;

-- A public SECURITY INVOKER function creates the chat and its participants atomically.
create or replace function public.face_video_create_chat(
    p_member_ids uuid[],
    p_title text default null,
    p_is_group boolean default false
)
returns uuid
language plpgsql
security invoker
set search_path = public, pg_temp
as $$
declare
    v_chat_id uuid;
    v_members uuid[];
    v_count integer;
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    select array_agg(distinct member_id)
      into v_members
      from unnest(coalesce(p_member_ids, array[]::uuid[]) || array[(select auth.uid())]) as member_id;

    v_count := coalesce(array_length(v_members, 1), 0);
    if v_count < 2 or v_count > 50 then
        raise exception 'A chat must have between 2 and 50 members';
    end if;
    if not p_is_group and v_count <> 2 then
        raise exception 'A direct chat must have exactly 2 members';
    end if;

    insert into public.face_video_chats (title, is_group, created_by)
    values (
        case when p_is_group then nullif(left(trim(coalesce(p_title, '')), 80), '') else null end,
        p_is_group,
        (select auth.uid())
    )
    returning id into v_chat_id;

    insert into public.face_video_chat_members (chat_id, user_id, role)
    select v_chat_id, member_id,
           case when member_id = (select auth.uid()) then 'owner' else 'member' end
    from unnest(v_members) as member_id;

    return v_chat_id;
end;
$$;

revoke all on function public.face_video_create_chat(uuid[], text, boolean) from public, anon;
grant execute on function public.face_video_create_chat(uuid[], text, boolean) to authenticated;

-- Only users whose sign-up metadata explicitly identifies Face Video receive an app profile.
create or replace function face_video_private.face_video_handle_new_auth_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if coalesce(new.raw_user_meta_data ->> 'face_video_app', 'false') = 'true' then
        insert into public.face_video_profiles (id, username, display_name)
        values (
            new.id,
            lower(new.raw_user_meta_data ->> 'username'),
            coalesce(new.raw_user_meta_data ->> 'display_name', '')
        )
        on conflict (id) do nothing;
    end if;
    return new;
end;
$$;

revoke all on function face_video_private.face_video_handle_new_auth_user() from public, anon, authenticated;
drop trigger if exists on_face_video_auth_user_created on auth.users;
create trigger on_face_video_auth_user_created
after insert on auth.users
for each row execute procedure face_video_private.face_video_handle_new_auth_user();

create or replace function face_video_private.face_video_update_chat_preview()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    update public.face_video_chats
       set last_message_preview = left(new.body, 160),
           last_message_at = new.created_at
     where id = new.chat_id;
    return new;
end;
$$;

revoke all on function face_video_private.face_video_update_chat_preview() from public, anon, authenticated;
drop trigger if exists after_face_video_message_insert on public.face_video_messages;
create trigger after_face_video_message_insert
after insert on public.face_video_messages
for each row execute procedure face_video_private.face_video_update_chat_preview();

create or replace view public.face_video_chat_summaries
with (security_invoker = true)
as
select
    c.id,
    case
        when c.is_group then coalesce(c.title, 'Групповой чат')
        else coalesce(
            (
                select coalesce(nullif(p.display_name, ''), '@' || p.username)
                from public.face_video_chat_members other_member
                join public.face_video_profiles p on p.id = other_member.user_id
                where other_member.chat_id = c.id
                  and other_member.user_id <> (select auth.uid())
                limit 1
            ),
            'Личный чат'
        )
    end as title,
    c.is_group,
    c.last_message_preview,
    c.last_message_at,
    c.created_at
from public.face_video_chats c
join public.face_video_chat_members current_member on current_member.chat_id = c.id
where current_member.user_id = (select auth.uid());

alter table public.face_video_profiles enable row level security;
alter table public.face_video_chats enable row level security;
alter table public.face_video_chat_members enable row level security;
alter table public.face_video_messages enable row level security;

drop policy if exists "face_video_profiles_read" on public.face_video_profiles;
create policy "face_video_profiles_read"
on public.face_video_profiles for select to authenticated
using ((select auth.uid()) is not null);

drop policy if exists "face_video_profiles_insert_own" on public.face_video_profiles;
create policy "face_video_profiles_insert_own"
on public.face_video_profiles for insert to authenticated
with check (id = (select auth.uid()));

drop policy if exists "face_video_profiles_update_own" on public.face_video_profiles;
create policy "face_video_profiles_update_own"
on public.face_video_profiles for update to authenticated
using (id = (select auth.uid()))
with check (id = (select auth.uid()));

drop policy if exists "face_video_chats_read_members" on public.face_video_chats;
create policy "face_video_chats_read_members"
on public.face_video_chats for select to authenticated
using (face_video_private.face_video_is_chat_member(id));

drop policy if exists "face_video_chats_create_own" on public.face_video_chats;
create policy "face_video_chats_create_own"
on public.face_video_chats for insert to authenticated
with check (created_by = (select auth.uid()));

drop policy if exists "face_video_members_read_self" on public.face_video_chat_members;
create policy "face_video_members_read_self"
on public.face_video_chat_members for select to authenticated
using (face_video_private.face_video_is_chat_member(chat_id));

drop policy if exists "face_video_members_insert_by_creator" on public.face_video_chat_members;
create policy "face_video_members_insert_by_creator"
on public.face_video_chat_members for insert to authenticated
with check (face_video_private.face_video_is_chat_creator(chat_id));

drop policy if exists "face_video_messages_read_members" on public.face_video_messages;
create policy "face_video_messages_read_members"
on public.face_video_messages for select to authenticated
using (face_video_private.face_video_is_chat_member(chat_id));

drop policy if exists "face_video_messages_insert_as_self" on public.face_video_messages;
create policy "face_video_messages_insert_as_self"
on public.face_video_messages for insert to authenticated
with check (
    sender_id = (select auth.uid())
    and face_video_private.face_video_is_chat_member(chat_id)
);

grant select, insert, update on public.face_video_profiles to authenticated;
grant select, insert on public.face_video_chats to authenticated;
grant select, insert on public.face_video_chat_members to authenticated;
grant select, insert on public.face_video_messages to authenticated;
grant select on public.face_video_chat_summaries to authenticated;

-- Add only the Face Video message table to the existing Realtime publication.
do $$
begin
    if not exists (
        select 1
        from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'face_video_messages'
    ) then
        alter publication supabase_realtime add table public.face_video_messages;
    end if;
end;
$$;
