
revoke all on table public.face_video_scheduled_messages from anon, authenticated;

grant select, delete
    on table public.face_video_scheduled_messages to authenticated;

grant insert (chat_id, body, scheduled_for)
    on table public.face_video_scheduled_messages to authenticated;

grant update (body, scheduled_for)
    on table public.face_video_scheduled_messages to authenticated;
