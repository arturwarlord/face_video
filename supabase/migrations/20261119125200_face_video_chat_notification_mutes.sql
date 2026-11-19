-- Per-user, per-chat notification mute settings.
-- The presence of a row means notifications are muted for that user and chat.

create table public.face_video_chat_notification_settings (
    user_id uuid not null references auth.users(id) on delete cascade,
    chat_id uuid not null references public.face_video_chats(id) on delete cascade,
    muted_at timestamptz not null default now(),
    primary key (user_id, chat_id)
);

create index face_video_chat_notification_settings_chat_user_idx
    on public.face_video_chat_notification_settings (chat_id, user_id);

alter table public.face_video_chat_notification_settings enable row level security;

revoke all on table public.face_video_chat_notification_settings from public, anon;
grant select, insert, delete on table public.face_video_chat_notification_settings to authenticated;
grant select on table public.face_video_chat_notification_settings to service_role;

create policy face_video_chat_notification_settings_select_own_member
    on public.face_video_chat_notification_settings
    for select to authenticated
    using (
        user_id = (select auth.uid())
        and face_video_private.face_video_is_chat_member(chat_id)
    );

create policy face_video_chat_notification_settings_insert_own_member
    on public.face_video_chat_notification_settings
    for insert to authenticated
    with check (
        user_id = (select auth.uid())
        and face_video_private.face_video_is_chat_member(chat_id)
    );

create policy face_video_chat_notification_settings_delete_own_member
    on public.face_video_chat_notification_settings
    for delete to authenticated
    using (
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
        insert into public.face_video_chat_notification_settings (user_id, chat_id)
        values ((select auth.uid()), p_chat_id)
        on conflict (user_id, chat_id) do nothing;
        return true;
    end if;

    delete from public.face_video_chat_notification_settings
     where user_id = (select auth.uid())
       and chat_id = p_chat_id;
    return false;
end;
$function$;

revoke all on function public.face_video_set_chat_notification_mute(uuid, boolean) from public, anon;
grant execute on function public.face_video_set_chat_notification_mute(uuid, boolean) to authenticated;
