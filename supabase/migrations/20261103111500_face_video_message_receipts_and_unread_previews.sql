-- Per-recipient delivery/read receipts and an inbox preview of the last unread incoming message.
-- The sender can read receipt states; only the recipient may advance their own receipt.

create table if not exists public.face_video_message_receipts (
    id uuid primary key default gen_random_uuid(),
    message_id uuid not null references public.face_video_messages(id) on delete cascade,
    chat_id uuid not null references public.face_video_chats(id) on delete cascade,
    sender_id uuid not null references auth.users(id) on delete cascade,
    recipient_id uuid not null references auth.users(id) on delete cascade,
    delivered_at timestamptz,
    read_at timestamptz,
    created_at timestamptz not null default now(),
    constraint face_video_message_receipts_message_recipient_key
        unique (message_id, recipient_id),
    constraint face_video_message_receipts_sender_recipient_check
        check (sender_id <> recipient_id)
);

create index if not exists idx_face_video_message_receipts_chat_message
    on public.face_video_message_receipts(chat_id, message_id);
create index if not exists idx_face_video_message_receipts_recipient_chat
    on public.face_video_message_receipts(recipient_id, chat_id, read_at);
create index if not exists idx_face_video_message_receipts_sender
    on public.face_video_message_receipts(sender_id);

alter table public.face_video_message_receipts enable row level security;

drop policy if exists "face_video_receipts_select_participants"
    on public.face_video_message_receipts;
create policy "face_video_receipts_select_participants"
on public.face_video_message_receipts
for select to authenticated
using (
    face_video_private.face_video_is_chat_member(chat_id)
    and (
        recipient_id = (select auth.uid())
        or sender_id = (select auth.uid())
    )
);

drop policy if exists "face_video_receipts_update_recipient"
    on public.face_video_message_receipts;
create policy "face_video_receipts_update_recipient"
on public.face_video_message_receipts
for update to authenticated
using (
    recipient_id = (select auth.uid())
    and face_video_private.face_video_is_chat_member(chat_id)
)
with check (
    recipient_id = (select auth.uid())
    and face_video_private.face_video_is_chat_member(chat_id)
);

revoke all on table public.face_video_message_receipts from anon, authenticated;
grant select on table public.face_video_message_receipts to authenticated;
grant update (delivered_at, read_at) on table public.face_video_message_receipts to authenticated;

-- Backfill historic rows. Previous read watermarks provide the read baseline;
-- any message newer than that watermark starts as delivered but not read.
insert into public.face_video_message_receipts (
    message_id, chat_id, sender_id, recipient_id, delivered_at, read_at, created_at
)
select
    msg.id,
    msg.chat_id,
    msg.sender_id,
    member.user_id,
    msg.created_at,
    case
        when coalesce(read_state.last_read_at, member.joined_at) >= msg.created_at
        then coalesce(read_state.last_read_at, member.joined_at)
        else null
    end,
    msg.created_at
from public.face_video_messages msg
join public.face_video_chat_members member
    on member.chat_id = msg.chat_id
   and member.user_id <> msg.sender_id
left join public.face_video_chat_read_state read_state
    on read_state.chat_id = msg.chat_id
   and read_state.user_id = member.user_id
on conflict (message_id, recipient_id) do nothing;

create or replace function face_video_private.face_video_create_message_receipts()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.face_video_message_receipts (
        message_id, chat_id, sender_id, recipient_id
    )
    select new.id, new.chat_id, new.sender_id, member.user_id
    from public.face_video_chat_members member
    where member.chat_id = new.chat_id
      and member.user_id <> new.sender_id
    on conflict (message_id, recipient_id) do nothing;

    return new;
end;
$$;

revoke all on function face_video_private.face_video_create_message_receipts()
    from public, anon, authenticated;
drop trigger if exists after_face_video_message_receipts
    on public.face_video_messages;
create trigger after_face_video_message_receipts
after insert on public.face_video_messages
for each row execute procedure face_video_private.face_video_create_message_receipts();

create or replace function public.face_video_get_unread_summaries()
returns table (
    chat_id uuid,
    unread_count bigint,
    last_unread_body text,
    last_unread_at timestamptz
)
language sql
stable
security invoker
set search_path = public, pg_temp
as $$
    select
        member.chat_id,
        count(msg.id)::bigint as unread_count,
        (array_agg(msg.body order by msg.created_at desc))[1] as last_unread_body,
        max(msg.created_at) as last_unread_at
    from public.face_video_chat_members member
    left join public.face_video_chat_read_state read_state
        on read_state.chat_id = member.chat_id
       and read_state.user_id = member.user_id
    left join public.face_video_messages msg
        on msg.chat_id = member.chat_id
       and msg.sender_id <> (select auth.uid())
       and msg.created_at > coalesce(read_state.last_read_at, member.joined_at)
    where member.user_id = (select auth.uid())
    group by member.chat_id;
$$;

revoke all on function public.face_video_get_unread_summaries() from public, anon;
grant execute on function public.face_video_get_unread_summaries() to authenticated;

create or replace function public.face_video_mark_messages_delivered(p_message_ids uuid[])
returns void
language plpgsql
security invoker
set search_path = public, pg_temp
as $$
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    update public.face_video_message_receipts
       set delivered_at = coalesce(delivered_at, now())
     where recipient_id = (select auth.uid())
       and message_id = any(coalesce(p_message_ids, array[]::uuid[]))
       and delivered_at is null;
end;
$$;

revoke all on function public.face_video_mark_messages_delivered(uuid[]) from public, anon;
grant execute on function public.face_video_mark_messages_delivered(uuid[]) to authenticated;

-- Opening a chat advances its unread watermark and marks every incoming
-- message receipt in the chat as both delivered and read.
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

    update public.face_video_message_receipts
       set delivered_at = coalesce(delivered_at, now()),
           read_at = coalesce(read_at, now())
     where chat_id = p_chat_id
       and recipient_id = (select auth.uid())
       and read_at is null;
end;
$$;

revoke all on function public.face_video_mark_chat_read(uuid) from public, anon;
grant execute on function public.face_video_mark_chat_read(uuid) to authenticated;

-- Receipt updates are consumed live by the sender's open conversation.
do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'face_video_message_receipts'
    ) then
        alter publication supabase_realtime
            add table public.face_video_message_receipts;
    end if;
end;
$$;
