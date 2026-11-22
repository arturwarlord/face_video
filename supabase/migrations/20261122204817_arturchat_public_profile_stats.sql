create or replace function public.face_video_get_profile_stats(p_user_id uuid)
returns table (
    friend_count bigint,
    story_count bigint,
    group_count bigint
)
language sql
stable
security definer
set search_path = ''
as $function$
    select
        (
            select count(*)
            from public.face_video_friendships f
            where f.status = 'accepted'
              and (f.user_low = p.id or f.user_high = p.id)
        ) as friend_count,
        (
            select count(*)
            from public.face_video_stories s
            where s.user_id = p.id
              and s.expires_at > now()
        ) as story_count,
        (
            select count(*)
            from public.face_video_chat_members cm
            join public.face_video_chats c on c.id = cm.chat_id
            where cm.user_id = p.id
              and c.is_group = true
        ) as group_count
    from public.face_video_profiles p
    where p.id = p_user_id
      and (select auth.uid()) is not null;
$function$;

revoke all on function public.face_video_get_profile_stats(uuid)
    from public, anon, authenticated;
grant execute on function public.face_video_get_profile_stats(uuid)
    to authenticated;

comment on function public.face_video_get_profile_stats(uuid) is
    'Returns only aggregate friend, active-story and group counts for an ArturChat profile; never returns private friend or group membership rows.';
