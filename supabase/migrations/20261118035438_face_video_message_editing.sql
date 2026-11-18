-- Allow message authors to edit their own text-only messages safely.
alter table public.face_video_messages
    add column if not exists edited_at timestamptz;

drop policy if exists face_video_messages_edit_own_text
    on public.face_video_messages;

create policy face_video_messages_edit_own_text
    on public.face_video_messages
    for update
    to authenticated
    using (
        sender_id = (select auth.uid())
        and deleted_at is null
        and attachment_path is null
        and char_length(btrim(body)) between 1 and 4000
        and face_video_private.face_video_is_chat_member(chat_id)
    )
    with check (
        sender_id = (select auth.uid())
        and deleted_at is null
        and attachment_path is null
        and char_length(btrim(body)) between 1 and 4000
        and face_video_private.face_video_is_chat_member(chat_id)
    );

-- Keep only the columns required by text editing and the existing soft-delete flow.
revoke update on table public.face_video_messages from authenticated;
grant update (body, edited_at, deleted_at, deleted_by)
    on table public.face_video_messages to authenticated;

create or replace function public.face_video_edit_message(
    p_message_id uuid,
    p_chat_id uuid,
    p_body text
)
returns boolean
language plpgsql
security invoker
set search_path = ''
as $function$
declare
    v_body text := btrim(coalesce(p_body, ''));
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    if not face_video_private.face_video_is_chat_member(p_chat_id) then
        raise exception 'Chat membership required';
    end if;

    if char_length(v_body) not between 1 and 4000 then
        raise exception 'Сообщение должно содержать от 1 до 4000 символов.';
    end if;

    update public.face_video_messages
       set body = v_body,
           edited_at = pg_catalog.now()
     where id = p_message_id
       and chat_id = p_chat_id
       and sender_id = (select auth.uid())
       and deleted_at is null
       and attachment_path is null;

    if not found then
        raise exception 'Можно редактировать только своё неудалённое текстовое сообщение.';
    end if;

    return true;
end;
$function$;

revoke all on function public.face_video_edit_message(uuid, uuid, text) from public, anon;
grant execute on function public.face_video_edit_message(uuid, uuid, text) to authenticated;
