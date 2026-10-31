create index if not exists idx_face_video_chats_created_by
  on public.face_video_chats(created_by);

create index if not exists idx_face_video_messages_sender_id
  on public.face_video_messages(sender_id);
