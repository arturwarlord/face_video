alter table public.face_video_profiles
  add column if not exists status_text text not null default '',
  add column if not exists about text not null default '';

alter table public.face_video_profiles
  add constraint face_video_profiles_status_text_length
    check (char_length(status_text) <= 80),
  add constraint face_video_profiles_about_length
    check (char_length(about) <= 500);

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('arturchat-avatars', 'arturchat-avatars', true, 5242880, array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set
  public = true,
  file_size_limit = 5242880,
  allowed_mime_types = array['image/jpeg','image/png','image/webp'];

drop policy if exists "ArturChat avatar upload own folder" on storage.objects;
create policy "ArturChat avatar upload own folder"
on storage.objects for insert to authenticated
with check (
  bucket_id = 'arturchat-avatars'
  and (storage.foldername(name))[1] = (select auth.uid())::text
);

drop policy if exists "ArturChat avatar update own folder" on storage.objects;
create policy "ArturChat avatar update own folder"
on storage.objects for update to authenticated
using (
  bucket_id = 'arturchat-avatars'
  and (storage.foldername(name))[1] = (select auth.uid())::text
)
with check (
  bucket_id = 'arturchat-avatars'
  and (storage.foldername(name))[1] = (select auth.uid())::text
);

grant select, insert, update on public.face_video_profiles to authenticated;
