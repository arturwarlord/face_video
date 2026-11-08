-- Voice messages extend the existing private chat-attachments bucket.
-- Existing Storage RLS policies continue to authorize chat members and uploader folders.

alter table public.face_video_messages
    add column if not exists attachment_duration_ms bigint;

alter table public.face_video_messages
    drop constraint if exists face_video_messages_attachment_metadata_check;

alter table public.face_video_messages
    add constraint face_video_messages_attachment_metadata_check
    check (
        (
            attachment_path is null
            and attachment_mime_type is null
            and attachment_file_name is null
            and attachment_size is null
            and attachment_duration_ms is null
        )
        or
        (
            attachment_path is not null
            and attachment_mime_type is not null
            and attachment_file_name is not null
            and char_length(attachment_file_name) between 1 and 255
            and attachment_size is not null
            and attachment_size between 1 and 10485760
            and attachment_path like chat_id::text || '/' || sender_id::text || '/%'
            and (
                (
                    attachment_mime_type = 'image/jpeg'
                    and attachment_duration_ms is null
                )
                or
                (
                    attachment_mime_type = 'audio/mp4'
                    and attachment_duration_ms is not null
                    and attachment_duration_ms between 1000 and 600000
                )
            )
        )
    );

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'chat-attachments',
    'chat-attachments',
    false,
    10485760,
    array['image/jpeg', 'audio/mp4']::text[]
)
on conflict (id) do update
set
    public = false,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;
