-- Allow the chat creator to read a chat during its initial creation transaction.
-- INSERT ... RETURNING id must also satisfy a SELECT policy before members exist.

drop policy if exists "face_video_chats_read_members"
  on public.face_video_chats;

create policy "face_video_chats_read_members"
on public.face_video_chats
for select
to authenticated
using (
  face_video_private.face_video_is_chat_member(id)
  or created_by = (select auth.uid())
);
