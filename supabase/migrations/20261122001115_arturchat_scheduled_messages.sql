create extension if not exists pg_cron with schema pg_catalog;

create table if not exists public.face_video_scheduled_messages (
    id uuid primary key default gen_random_uuid(),
    chat_id uuid not null references public.face_video_chats(id) on delete cascade,
    sender_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    body text not null,
    scheduled_for timestamptz not null,
    status text not null default 'pending',
    sent_message_id uuid references public.face_video_messages(id) on delete set null,
    created_at timestamptz not null default now(),
    sent_at timestamptz,
    failure_code text,
    constraint face_video_scheduled_messages_body_length
        check (char_length(btrim(body)) between 1 and 4000),
    constraint face_video_scheduled_messages_status
        check (status in ('pending', 'sent', 'cancelled', 'failed'))
);

create index if not exists idx_face_video_scheduled_messages_due
    on public.face_video_scheduled_messages (scheduled_for, id)
    where status = 'pending';

create index if not exists idx_face_video_scheduled_messages_owner_chat
    on public.face_video_scheduled_messages (sender_id, chat_id, scheduled_for desc);

alter table public.face_video_scheduled_messages enable row level security;

drop policy if exists face_video_scheduled_messages_select_own
    on public.face_video_scheduled_messages;
create policy face_video_scheduled_messages_select_own
    on public.face_video_scheduled_messages
    for select to authenticated
    using (sender_id = (select auth.uid()));

drop policy if exists face_video_scheduled_messages_insert_own_member
    on public.face_video_scheduled_messages;
create policy face_video_scheduled_messages_insert_own_member
    on public.face_video_scheduled_messages
    for insert to authenticated
    with check (
        sender_id = (select auth.uid())
        and status = 'pending'
        and face_video_private.face_video_is_chat_member(chat_id)
    );

drop policy if exists face_video_scheduled_messages_update_own_pending
    on public.face_video_scheduled_messages;
create policy face_video_scheduled_messages_update_own_pending
    on public.face_video_scheduled_messages
    for update to authenticated
    using (
        sender_id = (select auth.uid())
        and status = 'pending'
        and face_video_private.face_video_is_chat_member(chat_id)
    )
    with check (
        sender_id = (select auth.uid())
        and status = 'pending'
        and face_video_private.face_video_is_chat_member(chat_id)
    );

drop policy if exists face_video_scheduled_messages_delete_own_pending
    on public.face_video_scheduled_messages;
create policy face_video_scheduled_messages_delete_own_pending
    on public.face_video_scheduled_messages
    for delete to authenticated
    using (
        sender_id = (select auth.uid())
        and status = 'pending'
    );

grant select, insert, delete on public.face_video_scheduled_messages to authenticated;
grant update (body, scheduled_for)
    on public.face_video_scheduled_messages to authenticated;

create or replace function face_video_private.validate_scheduled_message()
returns trigger
language plpgsql
set search_path = ''
as $function$
begin
    if char_length(btrim(coalesce(new.body, ''))) not between 1 and 4000 then
        raise exception 'Сообщение должно содержать от 1 до 4000 символов.';
    end if;

    if new.scheduled_for <= pg_catalog.clock_timestamp() + interval '1 minute' then
        raise exception 'Выбери время отправки минимум через одну минуту.';
    end if;

    if new.scheduled_for > pg_catalog.clock_timestamp() + interval '365 days' then
        raise exception 'Нельзя планировать сообщение более чем на год вперёд.';
    end if;

    if tg_op = 'UPDATE' and old.status <> 'pending' then
        raise exception 'Можно менять только ожидающее отправки сообщение.';
    end if;

    return new;
end;
$function$;

drop trigger if exists face_video_validate_scheduled_message
    on public.face_video_scheduled_messages;
create trigger face_video_validate_scheduled_message
    before insert or update of body, scheduled_for
    on public.face_video_scheduled_messages
    for each row
    execute function face_video_private.validate_scheduled_message();

create or replace function face_video_private.dispatch_due_scheduled_messages()
returns integer
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_item record;
    v_message_id uuid;
    v_sent integer := 0;
begin
    for v_item in
        select scheduled.id, scheduled.chat_id, scheduled.sender_id, scheduled.body
          from public.face_video_scheduled_messages as scheduled
         where scheduled.status = 'pending'
           and scheduled.scheduled_for <= pg_catalog.clock_timestamp()
         order by scheduled.scheduled_for, scheduled.id
         limit 100
         for update skip locked
    loop
        if not exists (
            select 1
              from public.face_video_chat_members as member
             where member.chat_id = v_item.chat_id
               and member.user_id = v_item.sender_id
        ) then
            update public.face_video_scheduled_messages
               set status = 'cancelled',
                   failure_code = 'sender_no_longer_in_chat'
             where id = v_item.id;
            continue;
        end if;

        begin
            insert into public.face_video_messages (chat_id, sender_id, body)
            values (v_item.chat_id, v_item.sender_id, v_item.body)
            returning id into v_message_id;

            update public.face_video_scheduled_messages
               set status = 'sent',
                   sent_message_id = v_message_id,
                   sent_at = pg_catalog.clock_timestamp(),
                   failure_code = null
             where id = v_item.id
               and status = 'pending';

            v_sent := v_sent + 1;
        exception when others then
            update public.face_video_scheduled_messages
               set status = 'failed',
                   failure_code = sqlstate
             where id = v_item.id
               and status = 'pending';
        end;
    end loop;

    return v_sent;
end;
$function$;

revoke all on function face_video_private.validate_scheduled_message()
    from public, anon, authenticated;
revoke all on function face_video_private.dispatch_due_scheduled_messages()
    from public, anon, authenticated;

select cron.schedule(
    'arturchat-dispatch-scheduled-messages',
    '* * * * *',
    'select face_video_private.dispatch_due_scheduled_messages();'
);
