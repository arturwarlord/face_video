-- Index the new foreign keys so per-user hiding and message metadata remain efficient.
create index if not exists face_video_hidden_chats_chat_id_idx
  on public.face_video_hidden_chats(chat_id);
create index if not exists face_video_hidden_messages_message_id_idx
  on public.face_video_hidden_messages(message_id);
create index if not exists face_video_messages_deleted_by_idx
  on public.face_video_messages(deleted_by) where deleted_by is not null;
create index if not exists face_video_messages_forwarded_from_sender_idx
  on public.face_video_messages(forwarded_from_sender_id) where forwarded_from_sender_id is not null;
