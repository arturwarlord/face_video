drop policy if exists face_video_channel_members_read_self on public.face_video_channel_members;
drop policy if exists face_video_channel_members_leave_self on public.face_video_channel_members;

drop policy if exists face_video_channel_members_remove_by_manager on public.face_video_channel_members;
create policy face_video_channel_members_remove_by_manager
on public.face_video_channel_members
for delete
to authenticated
using (
    (user_id = (select auth.uid()) and role = 'subscriber')
    or (
        role = 'subscriber'
        and face_video_private.face_video_channel_has_permission(channel_id, 'remove_users')
    )
    or (
        role = 'admin'
        and face_video_private.face_video_is_channel_owner(channel_id)
    )
);
