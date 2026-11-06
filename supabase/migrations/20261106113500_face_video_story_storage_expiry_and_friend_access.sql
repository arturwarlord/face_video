drop policy if exists "ArturChat story read own and friends" on storage.objects;
create policy "ArturChat story read own and friends"
on storage.objects for select to authenticated
using (
    bucket_id = 'face-video-stories'
    and exists (
        select 1
        from public.face_video_stories s
        where s.media_path = objects.name
          and s.expires_at > now()
          and (
              s.user_id = (select auth.uid())
              or exists (
                  select 1
                  from public.face_video_friendships f
                  where f.status = 'accepted'
                    and (
                        (f.user_low = (select auth.uid()) and f.user_high = s.user_id)
                        or
                        (f.user_high = (select auth.uid()) and f.user_low = s.user_id)
                    )
              )
          )
    )
);
