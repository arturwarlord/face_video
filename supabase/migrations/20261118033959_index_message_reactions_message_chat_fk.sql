-- Cover the composite message/chat foreign key on message reactions.
create index if not exists face_video_message_reactions_message_chat_idx
    on public.face_video_message_reactions (message_id, chat_id);
