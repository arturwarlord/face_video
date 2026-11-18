-- Keep the inbox preview in sync when the most recent text message is edited.
create or replace function face_video_private.face_video_sync_edited_message_preview()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
    if new.edited_at is distinct from old.edited_at
       and new.deleted_at is null then
        update public.face_video_chats
           set last_message_preview = left(new.body, 160)
         where id = new.chat_id
           and last_message_at = new.created_at;
    end if;

    return new;
end;
$function$;

revoke all on function face_video_private.face_video_sync_edited_message_preview()
    from public, anon, authenticated;

drop trigger if exists after_face_video_message_edit_preview
    on public.face_video_messages;

create trigger after_face_video_message_edit_preview
after update of body, edited_at
on public.face_video_messages
for each row
execute function face_video_private.face_video_sync_edited_message_preview();
