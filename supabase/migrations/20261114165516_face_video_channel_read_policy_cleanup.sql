drop policy if exists face_video_channels_read_public_or_owner on public.face_video_channels;
drop policy if exists face_video_channels_read_management on public.face_video_channels;
create policy face_video_channels_read_public_owner_or_admin
on public.face_video_channels
for select
to authenticated
using (
    is_public = true
    or created_by = (select auth.uid())
    or face_video_private.face_video_channel_has_permission(id, 'change_info')
    or face_video_private.face_video_channel_has_permission(id, 'invite_users')
    or face_video_private.face_video_channel_has_permission(id, 'remove_users')
    or face_video_private.face_video_channel_has_permission(id, 'publish_posts')
);

drop policy if exists face_video_channel_posts_read_visible_channels on public.face_video_channel_posts;
drop policy if exists face_video_channel_posts_read_management on public.face_video_channel_posts;
create policy face_video_channel_posts_read_visible_or_manager
on public.face_video_channel_posts
for select
to authenticated
using (
    exists (
        select 1
        from public.face_video_channels c
        where c.id = channel_id
          and (
                c.is_public = true
                or c.created_by = (select auth.uid())
          )
    )
    or face_video_private.face_video_channel_has_permission(channel_id, 'publish_posts')
);
