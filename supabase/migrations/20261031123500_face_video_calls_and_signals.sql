-- Direct audio/video calls and private WebRTC signaling.

create table if not exists public.face_video_calls (
    id uuid primary key default gen_random_uuid(),
    caller_id uuid not null references auth.users(id) on delete cascade,
    callee_id uuid not null references auth.users(id) on delete cascade,
    media_type text not null check (media_type in ('audio', 'video')),
    status text not null default 'ringing'
        check (status in ('ringing', 'accepted', 'declined', 'cancelled', 'ended')),
    created_at timestamptz not null default now(),
    answered_at timestamptz,
    ended_at timestamptz,
    constraint face_video_calls_distinct_users check (caller_id <> callee_id)
);

create table if not exists public.face_video_call_signals (
    id uuid primary key default gen_random_uuid(),
    call_id uuid not null references public.face_video_calls(id) on delete cascade,
    sender_id uuid not null references auth.users(id) on delete cascade,
    recipient_id uuid not null references auth.users(id) on delete cascade,
    signal_type text not null check (signal_type in ('offer', 'answer', 'ice')),
    payload jsonb not null,
    created_at timestamptz not null default now(),
    constraint face_video_call_signals_distinct_users check (sender_id <> recipient_id)
);

create index if not exists idx_face_video_calls_callee_status
    on public.face_video_calls(callee_id, status, created_at desc);
create index if not exists idx_face_video_calls_caller_created
    on public.face_video_calls(caller_id, created_at desc);
create index if not exists idx_face_video_signals_recipient_call
    on public.face_video_call_signals(recipient_id, call_id, created_at);

alter table public.face_video_calls enable row level security;
alter table public.face_video_call_signals enable row level security;

drop policy if exists face_video_calls_read_participants on public.face_video_calls;
create policy face_video_calls_read_participants
on public.face_video_calls for select to authenticated
using ((select auth.uid()) = caller_id or (select auth.uid()) = callee_id);

drop policy if exists face_video_calls_create_own on public.face_video_calls;
create policy face_video_calls_create_own
on public.face_video_calls for insert to authenticated
with check (
    caller_id = (select auth.uid())
    and caller_id <> callee_id
    and status = 'ringing'
    and exists (
        select 1 from public.face_video_profiles p where p.id = callee_id
    )
);

drop policy if exists face_video_calls_update_status on public.face_video_calls;
create policy face_video_calls_update_status
on public.face_video_calls for update to authenticated
using ((select auth.uid()) = caller_id or (select auth.uid()) = callee_id)
with check ((select auth.uid()) = caller_id or (select auth.uid()) = callee_id);

create or replace function face_video_private.face_video_guard_call_status()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.caller_id <> old.caller_id
       or new.callee_id <> old.callee_id
       or new.media_type <> old.media_type
       or new.created_at <> old.created_at then
        raise exception 'Call participants and media type are immutable';
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
            raise exception 'Only call participants can end a call';
        end if;
        new.ended_at := now();
    else
        raise exception 'Invalid call status transition: % -> %', old.status, new.status;
    end if;

    return new;
end;
$$;

revoke all on function face_video_private.face_video_guard_call_status() from public, anon, authenticated;
drop trigger if exists face_video_calls_guard_status on public.face_video_calls;
create trigger face_video_calls_guard_status
before update on public.face_video_calls
for each row execute procedure face_video_private.face_video_guard_call_status();

drop policy if exists face_video_call_signals_read_recipient on public.face_video_call_signals;
create policy face_video_call_signals_read_recipient
on public.face_video_call_signals for select to authenticated
using (
    recipient_id = (select auth.uid())
    and exists (
        select 1
        from public.face_video_calls c
        where c.id = call_id
          and (
              (c.caller_id = sender_id and c.callee_id = recipient_id)
              or
              (c.callee_id = sender_id and c.caller_id = recipient_id)
          )
    )
);

drop policy if exists face_video_call_signals_insert_participant
on public.face_video_call_signals;
create policy face_video_call_signals_insert_participant
on public.face_video_call_signals for insert to authenticated
with check (
    sender_id = (select auth.uid())
    and exists (
        select 1
        from public.face_video_calls c
        where c.id = call_id
          and c.status in ('ringing', 'accepted')
          and (
              (c.caller_id = sender_id and c.callee_id = recipient_id)
              or
              (c.callee_id = sender_id and c.caller_id = recipient_id)
          )
    )
);

grant select, insert on public.face_video_calls to authenticated;
grant update(status) on public.face_video_calls to authenticated;
grant select, insert on public.face_video_call_signals to authenticated;

do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'face_video_calls'
    ) then
        alter publication supabase_realtime add table public.face_video_calls;
    end if;

    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'face_video_call_signals'
    ) then
        alter publication supabase_realtime add table public.face_video_call_signals;
    end if;
end;
$$;
