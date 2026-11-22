
revoke insert on public.face_video_scheduled_messages from authenticated;
grant insert (chat_id, body, scheduled_for)
    on public.face_video_scheduled_messages to authenticated;
