-- Store actual voice-message amplitude bars so waveform rendering is identical for all chat members.
alter table public.face_video_messages
    add column if not exists attachment_waveform jsonb;

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
            and attachment_waveform is null
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
                    and attachment_waveform is null
                )
                or
                (
                    attachment_mime_type = 'audio/mp4'
                    and attachment_duration_ms is not null
                    and attachment_duration_ms between 1000 and 600000
                    and (
                        attachment_waveform is null
                        or (
                            jsonb_typeof(attachment_waveform) = 'array'
                            and jsonb_array_length(attachment_waveform) between 16 and 96
                        )
                    )
                )
            )
        )
    );
