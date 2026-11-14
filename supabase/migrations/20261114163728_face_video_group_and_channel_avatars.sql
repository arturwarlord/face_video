alter table public.face_video_chats
    add column if not exists avatar_url text;

alter table public.face_video_channels
    add column if not exists avatar_url text;

create or replace view public.face_video_chat_summaries
with (security_invoker = true)
as
select
    c.id,
    case
        when c.is_group then coalesce(c.title, 'Групповой чат'::text)
        else coalesce((
            select coalesce(nullif(p.display_name, ''::text), '@'::text || p.username)
            from public.face_video_chat_members other_member
            join public.face_video_profiles p on p.id = other_member.user_id
            where other_member.chat_id = c.id
              and other_member.user_id <> (select auth.uid())
            limit 1
        ), 'Личный чат'::text)
    end as title,
    c.is_group,
    c.last_message_preview,
    c.last_message_at,
    c.created_at,
    c.avatar_url
from public.face_video_chats c
join public.face_video_chat_members current_member on current_member.chat_id = c.id
where current_member.user_id = (select auth.uid())
  and not exists (
      select 1
      from public.face_video_hidden_chats hidden
      where hidden.chat_id = c.id
        and hidden.user_id = (select auth.uid())
  );

grant update (avatar_url) on public.face_video_chats to authenticated;
grant update (avatar_url) on public.face_video_channels to authenticated;

drop policy if exists "ArturChat group and channel avatar upload" on storage.objects;
create policy "ArturChat group and channel avatar upload"
on storage.objects
for insert
to authenticated
with check (
    bucket_id = 'arturchat-avatars'
    and (
        (
            (storage.foldername(name))[1] = 'groups'
            and case
                when (storage.foldername(name))[2] ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                then face_video_private.face_video_can_manage_group_info(((storage.foldername(name))[2])::uuid)
                else false
            end
        )
        or
        (
            (storage.foldername(name))[1] = 'channels'
            and case
                when (storage.foldername(name))[2] ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                then exists (
                    select 1
                    from public.face_video_channels c
                    where c.id = ((storage.foldername(name))[2])::uuid
                      and c.created_by = (select auth.uid())
                )
                else false
            end
        )
    )
);

drop policy if exists "ArturChat group and channel avatar delete" on storage.objects;
create policy "ArturChat group and channel avatar delete"
on storage.objects
for delete
to authenticated
using (
    bucket_id = 'arturchat-avatars'
    and (
        (
            (storage.foldername(name))[1] = 'groups'
            and case
                when (storage.foldername(name))[2] ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                then face_video_private.face_video_can_manage_group_info(((storage.foldername(name))[2])::uuid)
                else false
            end
        )
        or
        (
            (storage.foldername(name))[1] = 'channels'
            and case
                when (storage.foldername(name))[2] ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
                then exists (
                    select 1
                    from public.face_video_channels c
                    where c.id = ((storage.foldername(name))[2])::uuid
                      and c.created_by = (select auth.uid())
                )
                else false
            end
        )
    )
);

comment on column public.face_video_chats.avatar_url is
    'Public ArturChat avatar URL for group conversations. Uploads are restricted to group managers.';
comment on column public.face_video_channels.avatar_url is
    'Public ArturChat avatar URL for channels. Uploads are restricted to channel owners.';
