-- Private image attachments for ArturChat conversations.
-- Files are stored outside Postgres and are readable only by authenticated
-- members of the conversation.

alter table public.face_video_messages
    add column if not exists attachment_path text,
    add column if not exists attachment_mime_type text,
    add column if not exists attachment_file_name text,
    add column if not exists attachment_size bigint;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'face_video_messages_attachment_metadata_check'
          and conrelid = 'public.face_video_messages'::regclass
    ) then
        alter table public.face_video_messages
            add constraint face_video_messages_attachment_metadata_check
            check (
                (
                    attachment_path is null
                    and attachment_mime_type is null
                    and attachment_file_name is null
                    and attachment_size is null
                )
                or
                (
                    attachment_path is not null
                    and attachment_mime_type = 'image/jpeg'
                    and attachment_file_name is not null
                    and char_length(attachment_file_name) between 1 and 255
                    and attachment_size between 1 and 10485760
                    and attachment_path like chat_id::text || '/' || sender_id::text || '/%'
                )
            );
    end if;
end
$$;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'chat-attachments',
    'chat-attachments',
    false,
    10485760,
    array['image/jpeg']::text[]
)
on conflict (id) do update
set
    public = false,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

create policy "chat attachments readable by chat members"
on storage.objects
for select
to authenticated
using (
    bucket_id = 'chat-attachments'
    and case
        when (storage.foldername(name))[1] ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
        then face_video_private.face_video_is_chat_member(((storage.foldername(name))[1])::uuid)
        else false
    end
);

create policy "chat members upload own chat attachments"
on storage.objects
for insert
to authenticated
with check (
    bucket_id = 'chat-attachments'
    and (storage.foldername(name))[2] = (select auth.uid())::text
    and case
        when (storage.foldername(name))[1] ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
        then face_video_private.face_video_is_chat_member(((storage.foldername(name))[1])::uuid)
        else false
    end
);

create policy "senders can delete own chat attachments"
on storage.objects
for delete
to authenticated
using (
    bucket_id = 'chat-attachments'
    and (storage.foldername(name))[2] = (select auth.uid())::text
    and case
        when (storage.foldername(name))[1] ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
        then face_video_private.face_video_is_chat_member(((storage.foldername(name))[1])::uuid)
        else false
    end
);
