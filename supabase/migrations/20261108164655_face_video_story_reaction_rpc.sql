create or replace function public.face_video_set_story_reaction(
    p_story_id uuid,
    p_reaction text
)
returns boolean
language plpgsql
security invoker
set search_path = ''
as $function$
declare
    v_story_owner uuid;
begin
    if p_reaction is null then
        delete from public.face_video_story_reactions
        where story_id = p_story_id
          and user_id = (select auth.uid());
        return true;
    end if;

    if p_reaction not in ('❤️', '😂', '😮', '😢', '🔥', '👏') then
        raise exception 'Unsupported story reaction';
    end if;

    select story.user_id
      into v_story_owner
      from public.face_video_stories as story
     where story.id = p_story_id
       and story.expires_at > pg_catalog.now()
       and story.user_id <> (select auth.uid());

    if v_story_owner is null then
        raise exception 'Story is unavailable for reaction';
    end if;

    insert into public.face_video_story_reactions (story_id, user_id, reaction)
    values (p_story_id, (select auth.uid()), p_reaction)
    on conflict (story_id, user_id)
    do update
       set reaction = excluded.reaction,
           created_at = pg_catalog.now();

    return true;
end;
$function$;

revoke all on function public.face_video_set_story_reaction(uuid, text) from public, anon;
grant execute on function public.face_video_set_story_reaction(uuid, text) to authenticated;
