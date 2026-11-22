create table if not exists public.face_video_group_topics (
    id uuid primary key default gen_random_uuid(),
    chat_id uuid not null references public.face_video_chats(id) on delete cascade,
    title text not null,
    icon text not null default '💬',
    sort_order integer not null default 0,
    created_by uuid not null default auth.uid() references auth.users(id),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint face_video_group_topics_title_length
        check (char_length(btrim(title)) between 2 and 48),
    constraint face_video_group_topics_icon_length
        check (char_length(icon) between 1 and 8)
);

create unique index if not exists idx_face_video_group_topics_unique_title
    on public.face_video_group_topics (chat_id, lower(btrim(title)));
create index if not exists idx_face_video_group_topics_chat_order
    on public.face_video_group_topics (chat_id, sort_order, created_at, id);
create index if not exists idx_face_video_group_topics_created_by
    on public.face_video_group_topics (created_by);

alter table public.face_video_group_topics enable row level security;

drop policy if exists face_video_group_topics_select_members
    on public.face_video_group_topics;
create policy face_video_group_topics_select_members
    on public.face_video_group_topics
    for select to authenticated
    using (
        face_video_private.face_video_is_chat_member(chat_id)
        and exists (
            select 1 from public.face_video_chats as group_chat
             where group_chat.id = face_video_group_topics.chat_id
               and group_chat.is_group = true
        )
    );

drop policy if exists face_video_group_topics_insert_managers
    on public.face_video_group_topics;
create policy face_video_group_topics_insert_managers
    on public.face_video_group_topics
    for insert to authenticated
    with check (
        created_by = (select auth.uid())
        and face_video_private.face_video_can_manage_group_info(chat_id)
        and exists (
            select 1 from public.face_video_chats as group_chat
             where group_chat.id = face_video_group_topics.chat_id
               and group_chat.is_group = true
        )
    );

drop policy if exists face_video_group_topics_update_managers
    on public.face_video_group_topics;
create policy face_video_group_topics_update_managers
    on public.face_video_group_topics
    for update to authenticated
    using (face_video_private.face_video_can_manage_group_info(chat_id))
    with check (
        face_video_private.face_video_can_manage_group_info(chat_id)
        and exists (
            select 1 from public.face_video_chats as group_chat
             where group_chat.id = face_video_group_topics.chat_id
               and group_chat.is_group = true
        )
    );

drop policy if exists face_video_group_topics_delete_managers
    on public.face_video_group_topics;
create policy face_video_group_topics_delete_managers
    on public.face_video_group_topics
    for delete to authenticated
    using (face_video_private.face_video_can_manage_group_info(chat_id));

grant select, insert, delete on public.face_video_group_topics to authenticated;
grant update (title, icon, sort_order, updated_at)
    on public.face_video_group_topics to authenticated;

alter table public.face_video_messages
    add column if not exists topic_id uuid
    references public.face_video_group_topics(id) on delete set null;

create index if not exists idx_face_video_messages_chat_topic_created
    on public.face_video_messages (chat_id, topic_id, created_at, id);
create index if not exists idx_face_video_messages_topic_id
    on public.face_video_messages (topic_id);

create or replace function face_video_private.validate_message_topic()
returns trigger
language plpgsql
set search_path = ''
as $function$
begin
    if new.topic_id is null then
        return new;
    end if;

    if not exists (
        select 1
          from public.face_video_group_topics as topic
         where topic.id = new.topic_id
           and topic.chat_id = new.chat_id
    ) then
        raise exception 'Тема не принадлежит этой группе.';
    end if;

    return new;
end;
$function$;

drop trigger if exists face_video_messages_validate_topic
    on public.face_video_messages;
create trigger face_video_messages_validate_topic
    before insert or update of chat_id, topic_id
    on public.face_video_messages
    for each row
    execute function face_video_private.validate_message_topic();

do $publication$
begin
    if exists (select 1 from pg_catalog.pg_publication where pubname = 'supabase_realtime')
       and not exists (
           select 1 from pg_catalog.pg_publication_tables
            where pubname = 'supabase_realtime'
              and schemaname = 'public'
              and tablename = 'face_video_group_topics'
       ) then
        execute 'alter publication supabase_realtime add table public.face_video_group_topics';
    end if;
end;
$publication$;
