-- Keep notification settings rows when unmuted so Realtime delivers state changes
-- as UPDATE events to every signed-in device.
alter table public.face_video_chat_notification_settings
    add column is_muted boolean not null default true;

grant update on table public.face_video_chat_notification_settings to authenticated;

create policy face_video_chat_notification_settings_update_own_member
    on public.face_video_chat_notification_settings
    for update to authenticated
    using (
        user_id = (select auth.uid())
        and face_video_private.face_video_is_chat_member(chat_id)
    )
    with check (
        user_id = (select auth.uid())
        and face_video_private.face_video_is_chat_member(chat_id)
    );

create or replace function public.face_video_set_chat_notification_mute(
    p_chat_id uuid,
    p_muted boolean
)
returns boolean
language plpgsql
security invoker
set search_path = ''
as $function$
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    if not face_video_private.face_video_is_chat_member(p_chat_id) then
        raise exception 'Chat membership required';
    end if;

    if p_muted then
        insert into public.face_video_chat_notification_settings (
            user_id, chat_id, is_muted, muted_at
        )
        values ((select auth.uid()), p_chat_id, true, pg_catalog.now())
        on conflict (user_id, chat_id)
        do update set
            is_muted = true,
            muted_at = pg_catalog.now();
        return true;
    end if;

    update public.face_video_chat_notification_settings
       set is_muted = false,
           muted_at = pg_catalog.now()
     where user_id = (select auth.uid())
       and chat_id = p_chat_id;
    return false;
end;
$function$;
