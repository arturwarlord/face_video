
drop policy if exists face_video_scheduled_messages_delete_own_pending
    on public.face_video_scheduled_messages;

create policy face_video_scheduled_messages_delete_own_pending
    on public.face_video_scheduled_messages
    for delete to authenticated
    using (
        sender_id = (select auth.uid())
        and status in ('pending', 'failed')
    );
