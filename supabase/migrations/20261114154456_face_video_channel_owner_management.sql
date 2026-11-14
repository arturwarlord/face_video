create policy face_video_channel_members_read_channel_owner
on public.face_video_channel_members
for select
to authenticated
using (
    exists (
        select 1
        from public.face_video_channels c
        where c.id = channel_id
          and c.created_by = (select auth.uid())
    )
);

create policy face_video_channel_posts_update_owner
on public.face_video_channel_posts
for update
to authenticated
using (
    author_id = (select auth.uid())
    and exists (
        select 1
        from public.face_video_channels c
        where c.id = channel_id
          and c.created_by = (select auth.uid())
    )
)
with check (
    author_id = (select auth.uid())
    and exists (
        select 1
        from public.face_video_channels c
        where c.id = channel_id
          and c.created_by = (select auth.uid())
    )
);

grant update on public.face_video_channel_posts to authenticated;

create or replace function public.face_video_set_channel_post_edited_at()
returns trigger
language plpgsql
set search_path to 'public', 'pg_temp'
as $function$
begin
    new.edited_at := now();
    return new;
end;
$function$;

revoke all on function public.face_video_set_channel_post_edited_at() from public, anon, authenticated;

drop trigger if exists face_video_channel_posts_set_edited_at
on public.face_video_channel_posts;

create trigger face_video_channel_posts_set_edited_at
before update on public.face_video_channel_posts
for each row
execute function public.face_video_set_channel_post_edited_at();
