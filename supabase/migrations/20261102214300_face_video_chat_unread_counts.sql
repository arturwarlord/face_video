-- Persist per-user/per-chat read watermarks and expose user-scoped unread counts.
-- Existing chats start with a clean unread baseline; new chats fall back to joined_at.

create table if not exists public.face_video_chat_read_state (
    chat_id uuid not null references public.face_video_chats(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    last_read_at timestamptz not null default now(),
    primary key (chat_id, user_id)
);

create index if not exists idx_face_video_chat_read_state_user
    on public.face_video_chat_read_state(user_id, chat_id);

alter table public.face_video_chat_read_state enable row level security;

drop policy if exists "face_video_chat_read_state_select_own"
    on public.face_video_chat_read_state;
create policy "face_video_chat_read_state_select_own"
on public.face_video_chat_read_state
for select to authenticated
using (
    user_id = (select auth.uid())
    and face_video_private.face_video_is_chat_member(chat_id)
);

drop policy if exists "face_video_chat_read_state_insert_own"
    on public.face_video_chat_read_state;
create policy "face_video_chat_read_state_insert_own"
on public.face_video_chat_read_state
for insert to authenticated
with check (
    user_id = (select auth.uid())
    and face_video_private.face_video_is_chat_member(chat_id)
);

drop policy if exists "face_video_chat_read_state_update_own"
    on public.face_video_chat_read_state;
create policy "face_video_chat_read_state_update_own"
on public.face_video_chat_read_state
for update to authenticated
using (
    user_id = (select auth.uid())
    and face_video_private.face_video_is_chat_member(chat_id)
)
with check (
    user_id = (select auth.uid())
    and face_video_private.face_video_is_chat_member(chat_id)
);

grant select, insert, update on public.face_video_chat_read_state to authenticated;

insert into public.face_video_chat_read_state (chat_id, user_id, last_read_at)
select chat_id, user_id, now()
from public.face_video_chat_members
on conflict (chat_id, user_id) do nothing;

create or replace function public.face_video_get_unread_counts()
returns table (chat_id uuid, unread_count bigint)
language sql
stable
security invoker
set search_path = public, pg_temp
as $$
    select
        cm.chat_id,
        count(msg.id)::bigint as unread_count
    from public.face_video_chat_members cm
    left join public.face_video_chat_read_state rs
        on rs.chat_id = cm.chat_id
       and rs.user_id = cm.user_id
    left join public.face_video_messages msg
        on msg.chat_id = cm.chat_id
       and msg.sender_id <> (select auth.uid())
       and msg.created_at > coalesce(rs.last_read_at, cm.joined_at)
    where cm.user_id = (select auth.uid())
    group by cm.chat_id;
$$;

revoke all on function public.face_video_get_unread_counts() from public, anon;
grant execute on function public.face_video_get_unread_counts() to authenticated;

create or replace function public.face_video_mark_chat_read(p_chat_id uuid)
returns void
language plpgsql
security invoker
set search_path = public, pg_temp
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    if not face_video_private.face_video_is_chat_member(p_chat_id) then
        raise exception 'Chat membership required';
    end if;

    insert into public.face_video_chat_read_state (chat_id, user_id, last_read_at)
    values (p_chat_id, (select auth.uid()), now())
    on conflict (chat_id, user_id)
    do update set last_read_at = excluded.last_read_at;
end;
$$;

revoke all on function public.face_video_mark_chat_read(uuid) from public, anon;
grant execute on function public.face_video_mark_chat_read(uuid) to authenticated;
