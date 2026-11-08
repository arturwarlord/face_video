create table public.face_video_story_reactions (
    story_id uuid not null references public.face_video_stories(id) on delete cascade,
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    reaction text not null,
    created_at timestamptz not null default now(),
    constraint face_video_story_reactions_pkey primary key (story_id, user_id),
    constraint face_video_story_reactions_allowed_reaction
        check (reaction in ('❤️', '😂', '😮', '😢', '🔥', '👏'))
);

create index face_video_story_reactions_user_id_idx
    on public.face_video_story_reactions (user_id);

alter table public.face_video_story_reactions enable row level security;

revoke all on table public.face_video_story_reactions from anon, public;
grant select, insert, update, delete on table public.face_video_story_reactions to authenticated;

create policy face_video_story_reactions_select_visible
    on public.face_video_story_reactions
    for select
    to authenticated
    using (
        exists (
            select 1
            from public.face_video_stories as story
            where story.id = face_video_story_reactions.story_id
        )
    );

create policy face_video_story_reactions_insert_friend_story
    on public.face_video_story_reactions
    for insert
    to authenticated
    with check (
        user_id = (select auth.uid())
        and exists (
            select 1
            from public.face_video_stories as story
            where story.id = face_video_story_reactions.story_id
              and story.user_id <> (select auth.uid())
        )
    );

create policy face_video_story_reactions_update_own
    on public.face_video_story_reactions
    for update
    to authenticated
    using (
        user_id = (select auth.uid())
        and exists (
            select 1
            from public.face_video_stories as story
            where story.id = face_video_story_reactions.story_id
              and story.user_id <> (select auth.uid())
        )
    )
    with check (
        user_id = (select auth.uid())
        and exists (
            select 1
            from public.face_video_stories as story
            where story.id = face_video_story_reactions.story_id
              and story.user_id <> (select auth.uid())
        )
    );

create policy face_video_story_reactions_delete_own
    on public.face_video_story_reactions
    for delete
    to authenticated
    using (user_id = (select auth.uid()));
