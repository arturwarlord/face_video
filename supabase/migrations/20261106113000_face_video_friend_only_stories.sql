create table if not exists public.face_video_stories (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    media_path text not null,
    caption text not null default '',
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    constraint face_video_stories_caption_length check (char_length(caption) <= 500),
    constraint face_video_stories_path_owner check (media_path like (user_id::text || '/%'))
);

create index if not exists idx_face_video_stories_user_expires
    on public.face_video_stories(user_id, expires_at desc);
create index if not exists idx_face_video_stories_expires
    on public.face_video_stories(expires_at);

alter table public.face_video_stories enable row level security;

drop policy if exists "face_video_stories_read_friends" on public.face_video_stories;
create policy "face_video_stories_read_friends"
on public.face_video_stories for select to authenticated
using (
    expires_at > now()
    and (
        user_id = (select auth.uid())
        or exists (
            select 1
            from public.face_video_friendships f
            where f.status = 'accepted'
              and (
                  (f.user_low = (select auth.uid()) and f.user_high = face_video_stories.user_id)
                  or
                  (f.user_high = (select auth.uid()) and f.user_low = face_video_stories.user_id)
              )
        )
    )
);

drop policy if exists "face_video_stories_insert_own" on public.face_video_stories;
create policy "face_video_stories_insert_own"
on public.face_video_stories for insert to authenticated
with check (
    user_id = (select auth.uid())
    and media_path like ((select auth.uid())::text || '/%')
    and expires_at > now()
);

drop policy if exists "face_video_stories_delete_own" on public.face_video_stories;
create policy "face_video_stories_delete_own"
on public.face_video_stories for delete to authenticated
using (user_id = (select auth.uid()));

revoke all on public.face_video_stories from anon, authenticated;
grant select, insert, delete on public.face_video_stories to authenticated;

create or replace function face_video_private.face_video_set_story_expiration()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.created_at := now();
    new.expires_at := now() + interval '24 hours';
    return new;
end;
$$;

drop trigger if exists face_video_stories_set_expiration on public.face_video_stories;
create trigger face_video_stories_set_expiration
before insert on public.face_video_stories
for each row execute function face_video_private.face_video_set_story_expiration();

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('face-video-stories', 'face-video-stories', false, 10485760, array['image/jpeg'])
on conflict (id) do update set
    public = false,
    file_size_limit = 10485760,
    allowed_mime_types = array['image/jpeg'];

drop policy if exists "ArturChat story upload own folder" on storage.objects;
create policy "ArturChat story upload own folder"
on storage.objects for insert to authenticated
with check (
    bucket_id = 'face-video-stories'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

drop policy if exists "ArturChat story read own and friends" on storage.objects;
create policy "ArturChat story read own and friends"
on storage.objects for select to authenticated
using (
    bucket_id = 'face-video-stories'
    and (
        (storage.foldername(name))[1] = (select auth.uid())::text
        or exists (
            select 1
            from public.face_video_friendships f
            where f.status = 'accepted'
              and (
                  (f.user_low = (select auth.uid()) and f.user_high::text = (storage.foldername(name))[1])
                  or
                  (f.user_high = (select auth.uid()) and f.user_low::text = (storage.foldername(name))[1])
              )
        )
    )
);

drop policy if exists "ArturChat story delete own folder" on storage.objects;
create policy "ArturChat story delete own folder"
on storage.objects for delete to authenticated
using (
    bucket_id = 'face-video-stories'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);
