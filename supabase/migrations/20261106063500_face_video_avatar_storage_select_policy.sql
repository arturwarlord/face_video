drop policy if exists "ArturChat avatar select own folder" on storage.objects;
create policy "ArturChat avatar select own folder"
on storage.objects for select to authenticated
using (
  bucket_id = 'arturchat-avatars'
  and (storage.foldername(name))[1] = (select auth.uid())::text
);
