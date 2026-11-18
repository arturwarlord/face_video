-- Consolidate text-edit and soft-delete UPDATE rules into one policy.
drop policy if exists face_video_messages_delete_own_for_all
    on public.face_video_messages;
drop policy if exists face_video_messages_edit_own_text
    on public.face_video_messages;

create policy face_video_messages_update_own
    on public.face_video_messages
    for update
    to authenticated
    using (
        sender_id = (select auth.uid())
        and deleted_at is null
        and face_video_private.face_video_is_chat_member(chat_id)
    )
    with check (
        (
            sender_id = (select auth.uid())
            and deleted_at is not null
            and deleted_by = (select auth.uid())
            and face_video_private.face_video_is_chat_member(chat_id)
        )
        or
        (
            sender_id = (select auth.uid())
            and deleted_at is null
            and attachment_path is null
            and edited_at is not null
            and char_length(btrim(body)) between 1 and 4000
            and face_video_private.face_video_is_chat_member(chat_id)
        )
    );
