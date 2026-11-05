-- The hidden row's foreign key validates message existence; RLS scopes hiding to the caller.
drop policy if exists "face_video_hidden_messages_insert_own" on public.face_video_hidden_messages;
create policy "face_video_hidden_messages_insert_own"
on public.face_video_hidden_messages for insert to authenticated
with check (user_id = (select auth.uid()));
