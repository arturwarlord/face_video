create table if not exists public.face_video_online_presence (
    user_id uuid primary key references auth.users(id) on delete cascade,
    last_seen_at timestamptz not null default now()
);

create index if not exists idx_face_video_online_presence_last_seen
    on public.face_video_online_presence(last_seen_at desc);

alter table public.face_video_online_presence enable row level security;

drop policy if exists face_video_online_presence_read on public.face_video_online_presence;
create policy face_video_online_presence_read
on public.face_video_online_presence
for select
to authenticated
using (true);

drop policy if exists face_video_online_presence_insert_own on public.face_video_online_presence;
create policy face_video_online_presence_insert_own
on public.face_video_online_presence
for insert
to authenticated
with check (user_id = (select auth.uid()));

drop policy if exists face_video_online_presence_update_own on public.face_video_online_presence;
create policy face_video_online_presence_update_own
on public.face_video_online_presence
for update
to authenticated
using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()));

grant select, insert, update on public.face_video_online_presence to authenticated;
