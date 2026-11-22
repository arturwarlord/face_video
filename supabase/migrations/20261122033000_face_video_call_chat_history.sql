-- Link every direct call to its exact conversation and keep a shared call log.
alter table public.face_video_calls
    add column if not exists chat_id uuid
        references public.face_video_chats(id) on delete set null;

create index if not exists idx_face_video_calls_chat_created
    on public.face_video_calls(chat_id, created_at desc);

-- Resolve historical calls only when a pair has exactly one common direct chat.
with direct_chat_candidates as (
    select caller_member.user_id as caller_id,
           callee_member.user_id as callee_id,
           chat_row.id as chat_id
    from public.face_video_chats chat_row
    join public.face_video_chat_members caller_member
      on caller_member.chat_id = chat_row.id
    join public.face_video_chat_members callee_member
      on callee_member.chat_id = chat_row.id
     and callee_member.user_id <> caller_member.user_id
    where chat_row.is_group = false
      and (
          select count(*)
          from public.face_video_chat_members member_count
          where member_count.chat_id = chat_row.id
      ) = 2
),
unique_direct_chats as (
    select caller_id, callee_id, min(chat_id) as chat_id
    from direct_chat_candidates
    group by caller_id, callee_id
    having count(*) = 1
)
update public.face_video_calls call_row
   set chat_id = direct_chat.chat_id
  from unique_direct_chats direct_chat
 where call_row.chat_id is null
   and call_row.caller_id = direct_chat.caller_id
   and call_row.callee_id = direct_chat.callee_id;

-- New calls must point to a direct chat containing exactly both participants.
drop policy if exists face_video_calls_create_own on public.face_video_calls;
create policy face_video_calls_create_own
on public.face_video_calls for insert to authenticated
with check (
    caller_id = (select auth.uid())
    and caller_id <> callee_id
    and status = 'ringing'
    and exists (
        select 1 from public.face_video_profiles profile_row
        where profile_row.id = callee_id
    )
    and exists (
        select 1
        from public.face_video_chats chat_row
        where chat_row.id = face_video_calls.chat_id
          and chat_row.is_group = false
          and exists (
              select 1
              from public.face_video_chat_members caller_member
              where caller_member.chat_id = chat_row.id
                and caller_member.user_id = caller_id
          )
          and exists (
              select 1
              from public.face_video_chat_members callee_member
              where callee_member.chat_id = chat_row.id
                and callee_member.user_id = callee_id
          )
          and (
              select count(*)
              from public.face_video_chat_members all_members
              where all_members.chat_id = chat_row.id
          ) = 2
    )
);

-- The chat link is immutable once the call has been created.
create or replace function face_video_private.face_video_guard_call_status()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.caller_id <> old.caller_id
       or new.callee_id <> old.callee_id
       or new.media_type <> old.media_type
       or new.created_at <> old.created_at
       or new.chat_id is distinct from old.chat_id then
        raise exception 'Call participants, chat and media type are immutable';
    end if;

    if new.status = old.status then
        return new;
    end if;

    if old.status = 'ringing' and new.status in ('accepted', 'declined') then
        if (select auth.uid()) <> old.callee_id then
            raise exception 'Only the invited user can answer or decline';
        end if;
        if new.status = 'accepted' then
            new.answered_at := now();
        else
            new.ended_at := now();
        end if;
    elsif old.status = 'ringing' and new.status = 'cancelled' then
        if (select auth.uid()) <> old.caller_id then
            raise exception 'Only the caller can cancel a ringing call';
        end if;
        new.ended_at := now();
    elsif old.status = 'accepted' and new.status = 'ended' then
        if (select auth.uid()) not in (old.caller_id, old.callee_id) then
            raise exception 'Only call participants can end the call';
        end if;
        new.ended_at := now();
    else
        raise exception 'Invalid call status transition: % -> %', old.status, new.status;
    end if;

    return new;
end;
$$;

revoke all on function face_video_private.face_video_guard_call_status()
    from public, anon, authenticated;

create table if not exists public.face_video_call_history (
    id uuid primary key default gen_random_uuid(),
    call_id uuid not null unique references public.face_video_calls(id) on delete cascade,
    chat_id uuid not null references public.face_video_chats(id) on delete cascade,
    caller_id uuid not null references auth.users(id) on delete cascade,
    callee_id uuid not null references auth.users(id) on delete cascade,
    media_type text not null check (media_type in ('audio', 'video')),
    status text not null check (status in ('ended', 'declined', 'cancelled')),
    created_at timestamptz not null,
    answered_at timestamptz,
    ended_at timestamptz,
    duration_seconds integer not null default 0 check (duration_seconds >= 0),
    constraint face_video_call_history_distinct_users check (caller_id <> callee_id)
);

create index if not exists idx_face_video_call_history_chat_created
    on public.face_video_call_history(chat_id, created_at desc);

alter table public.face_video_call_history enable row level security;

drop policy if exists face_video_call_history_read_participants
    on public.face_video_call_history;
create policy face_video_call_history_read_participants
on public.face_video_call_history for select to authenticated
using (
    ((select auth.uid()) = caller_id or (select auth.uid()) = callee_id)
    and face_video_private.face_video_is_chat_member(chat_id)
);

grant select on public.face_video_call_history to authenticated;
revoke insert, update, delete on public.face_video_call_history from anon, authenticated;

-- Only this private trigger can insert history; the unique call_id prevents duplicates.
create or replace function face_video_private.face_video_record_call_history()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.chat_id is null then
        return new;
    end if;

    insert into public.face_video_call_history (
        call_id, chat_id, caller_id, callee_id, media_type, status,
        created_at, answered_at, ended_at, duration_seconds
    )
    values (
        new.id, new.chat_id, new.caller_id, new.callee_id, new.media_type, new.status,
        coalesce(new.ended_at, now()), new.answered_at, new.ended_at,
        case
            when new.answered_at is not null and new.ended_at is not null
                then greatest(0, floor(extract(epoch from (new.ended_at - new.answered_at)))::integer)
            else 0
        end
    )
    on conflict (call_id) do nothing;

    return new;
end;
$$;

revoke all on function face_video_private.face_video_record_call_history()
    from public, anon, authenticated;

drop trigger if exists after_face_video_call_terminal_status
    on public.face_video_calls;
create trigger after_face_video_call_terminal_status
after update of status on public.face_video_calls
for each row
when (
    old.status is distinct from new.status
    and new.status in ('ended', 'declined', 'cancelled')
)
execute procedure face_video_private.face_video_record_call_history();

-- Keep the conversation list preview updated without creating normal message/push rows.
create or replace function face_video_private.face_video_update_chat_preview_from_call()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_preview text;
begin
    v_preview := case
        when new.status = 'ended' and new.media_type = 'video' then 'Видеозвонок'
        when new.status = 'ended' then 'Аудиозвонок'
        when new.status = 'declined' then 'Отклонённый вызов'
        else 'Пропущенный звонок'
    end;

    update public.face_video_chats
       set last_message_preview = v_preview,
           last_message_at = new.created_at
     where id = new.chat_id
       and (last_message_at is null or last_message_at <= new.created_at);

    return new;
end;
$$;

revoke all on function face_video_private.face_video_update_chat_preview_from_call()
    from public, anon, authenticated;

drop trigger if exists after_face_video_call_history_insert
    on public.face_video_call_history;
create trigger after_face_video_call_history_insert
after insert on public.face_video_call_history
for each row execute procedure face_video_private.face_video_update_chat_preview_from_call();

-- Backfill terminal calls where a unique direct chat was identified above.
insert into public.face_video_call_history (
    call_id, chat_id, caller_id, callee_id, media_type, status,
    created_at, answered_at, ended_at, duration_seconds
)
select
    call_row.id, call_row.chat_id, call_row.caller_id, call_row.callee_id,
    call_row.media_type, call_row.status,
    coalesce(call_row.ended_at, call_row.created_at),
    call_row.answered_at, call_row.ended_at,
    case
        when call_row.answered_at is not null and call_row.ended_at is not null
            then greatest(0, floor(extract(epoch from (call_row.ended_at - call_row.answered_at)))::integer)
        else 0
    end
from public.face_video_calls call_row
where call_row.chat_id is not null
  and call_row.status in ('ended', 'declined', 'cancelled')
on conflict (call_id) do nothing;

-- Expose the RLS-protected history table to authenticated Data API and Realtime clients.
do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'face_video_call_history'
    ) then
        alter publication supabase_realtime add table public.face_video_call_history;
    end if;
end;
$$;
