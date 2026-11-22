
create index if not exists idx_face_video_scheduled_messages_chat_id
    on public.face_video_scheduled_messages (chat_id);

create index if not exists idx_face_video_scheduled_messages_sent_message_id
    on public.face_video_scheduled_messages (sent_message_id)
    where sent_message_id is not null;
