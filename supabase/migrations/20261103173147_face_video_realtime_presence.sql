-- Authorize only authenticated ArturChat users to use the private online-presence channel.
-- Presence stores each live session in Realtime and automatically removes it on disconnect.

drop policy if exists "face_video_presence_read" on realtime.messages;
create policy "face_video_presence_read"
on realtime.messages
for select
to authenticated
using (
    realtime.topic() = 'face_video:online'
    and exists (
        select 1
        from public.face_video_profiles p
        where p.id = (select auth.uid())
    )
);

drop policy if exists "face_video_presence_write" on realtime.messages;
create policy "face_video_presence_write"
on realtime.messages
for insert
to authenticated
with check (
    realtime.topic() = 'face_video:online'
    and exists (
        select 1
        from public.face_video_profiles p
        where p.id = (select auth.uid())
    )
);
