alter table public.face_video_messages
    add column reply_to_message_id uuid;

alter table public.face_video_messages
    add constraint face_video_messages_reply_to_message_id_fkey
    foreign key (reply_to_message_id)
    references public.face_video_messages(id)
    on delete set null;

create index face_video_messages_reply_to_message_id_idx
    on public.face_video_messages (reply_to_message_id)
    where reply_to_message_id is not null;

create or replace function public.face_video_validate_reply_target()
returns trigger
language plpgsql
set search_path = ''
as $function$
declare
    target_chat_id uuid;
begin
    if new.reply_to_message_id is null then
        return new;
    end if;

    select message.chat_id
      into target_chat_id
      from public.face_video_messages as message
     where message.id = new.reply_to_message_id;

    if target_chat_id is null or target_chat_id <> new.chat_id then
        raise exception 'Ответ можно отправить только на сообщение из этой переписки.';
    end if;

    return new;
end;
$function$;

revoke all on function public.face_video_validate_reply_target() from public;

create trigger face_video_messages_validate_reply_target
before insert or update of chat_id, reply_to_message_id
on public.face_video_messages
for each row
execute function public.face_video_validate_reply_target();
