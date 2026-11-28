-- Reaction totals are private to the story author.
-- A viewer may read only their own reaction so the UI can restore their selection.
drop policy if exists face_video_story_reactions_select_visible
on public.face_video_story_reactions;

create policy face_video_story_reactions_select_owner_or_self
on public.face_video_story_reactions
for select
to authenticated
using (
    user_id = (select auth.uid())
    or exists (
        select 1
        from public.face_video_stories as story
        where story.id = face_video_story_reactions.story_id
          and story.user_id = (select auth.uid())
    )
);
