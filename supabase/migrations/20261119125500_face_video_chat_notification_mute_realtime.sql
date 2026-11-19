-- Notify signed-in clients when another device changes a chat's mute setting.
do $$
begin
    if not exists (
        select 1
        from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'face_video_chat_notification_settings'
    ) then
        alter publication supabase_realtime
            add table public.face_video_chat_notification_settings;
    end if;
end;
$$;
