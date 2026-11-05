-- Per-user message/chat hiding, soft delete-for-everyone, and forwarding metadata.
alter table public.face_video_messages
  add column if not exists deleted_at timestamptz,
  add column if not exists deleted_by uuid references auth.users(id) on delete set null,
  add column if not exists forwarded_from_sender_id uuid references auth.users(id) on delete set null,
  add column if not exists forwarded_from_name text;

create table if not exists public.face_video_hidden_messages (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  message_id uuid not null references public.face_video_messages(id) on delete cascade,
  hidden_at timestamptz not null default now(),
  primary key (user_id, message_id)
);
create table if not exists public.face_video_hidden_chats (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  chat_id uuid not null references public.face_video_chats(id) on delete cascade,
  hidden_at timestamptz not null default now(),
  primary key (user_id, chat_id)
);
alter table public.face_video_hidden_messages enable row level security;
alter table public.face_video_hidden_chats enable row level security;

create policy "face_video_hidden_messages_select_own" on public.face_video_hidden_messages
for select to authenticated using (user_id = (select auth.uid()));
create policy "face_video_hidden_messages_insert_own" on public.face_video_hidden_messages
for insert to authenticated with check (
  user_id = (select auth.uid()) and exists (
    select 1 from public.face_video_messages m
    where m.id = message_id and face_video_private.face_video_is_chat_member(m.chat_id)
  )
);
create policy "face_video_hidden_messages_delete_own" on public.face_video_hidden_messages
for delete to authenticated using (user_id = (select auth.uid()));

create policy "face_video_hidden_chats_select_own" on public.face_video_hidden_chats
for select to authenticated using (user_id = (select auth.uid()));
create policy "face_video_hidden_chats_insert_own" on public.face_video_hidden_chats
for insert to authenticated with check (
  user_id = (select auth.uid()) and face_video_private.face_video_is_chat_member(chat_id)
);
create policy "face_video_hidden_chats_delete_own" on public.face_video_hidden_chats
for delete to authenticated using (user_id = (select auth.uid()));

drop policy if exists "face_video_messages_read_members" on public.face_video_messages;
create policy "face_video_messages_read_members" on public.face_video_messages
for select to authenticated using (
  face_video_private.face_video_is_chat_member(chat_id)
  and not exists (
    select 1 from public.face_video_hidden_messages h
    where h.message_id = id and h.user_id = (select auth.uid())
  )
);
create policy "face_video_messages_delete_own_for_all" on public.face_video_messages
for update to authenticated
using (
  sender_id = (select auth.uid()) and deleted_at is null
  and face_video_private.face_video_is_chat_member(chat_id)
)
with check (
  sender_id = (select auth.uid()) and deleted_at is not null
  and deleted_by = (select auth.uid())
  and face_video_private.face_video_is_chat_member(chat_id)
);
grant select, insert, delete on public.face_video_hidden_messages to authenticated;
grant select, insert, delete on public.face_video_hidden_chats to authenticated;
grant update on public.face_video_messages to authenticated;

create or replace view public.face_video_chat_summaries
with (security_invoker = true) as
select c.id,
  case when c.is_group then coalesce(c.title, 'Групповой чат')
  else coalesce((
    select coalesce(nullif(p.display_name, ''), '@' || p.username)
    from public.face_video_chat_members other_member
    join public.face_video_profiles p on p.id = other_member.user_id
    where other_member.chat_id = c.id and other_member.user_id <> (select auth.uid())
    limit 1
  ), 'Личный чат') end as title,
  c.is_group, c.last_message_preview, c.last_message_at, c.created_at
from public.face_video_chats c
join public.face_video_chat_members current_member on current_member.chat_id = c.id
where current_member.user_id = (select auth.uid())
and not exists (
  select 1 from public.face_video_hidden_chats h
  where h.chat_id = c.id and h.user_id = (select auth.uid())
);
