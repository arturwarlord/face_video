-- Reactions for individual chat messages. A user can keep one reaction per message.
-- Composite foreign key guarantees that the message and chat IDs always match.

alter table public.face_video_messages
    add constraint face_video_messages_id_chat_id_unique
    unique (id, chat_id);

create table public.face_video_message_reactions (
    id uuid primary key default gen_random_uuid(),
    message_id uuid not null,
    chat_id uuid not null,
    user_id uuid not null default auth.uid()
        references auth.users(id) on delete cascade,
    reaction text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint face_video_message_reactions_message_user_unique
        unique (message_id, user_id),
    constraint face_video_message_reactions_message_chat_fkey
        foreign key (message_id, chat_id)
        references public.face_video_messages (id, chat_id)
        on delete cascade,
    constraint face_video_message_reactions_allowed_reaction
        check (reaction in ('👍', '❤️', '😂', '😮', '😢', '🔥', '👏', '👎'))
);

create index face_video_message_reactions_chat_message_idx
    on public.face_video_message_reactions (chat_id, message_id, created_at);

create index face_video_message_reactions_user_id_idx
    on public.face_video_message_reactions (user_id);

alter table public.face_video_message_reactions enable row level security;

revoke all on table public.face_video_message_reactions from anon, public;
grant select, insert, update, delete
    on table public.face_video_message_reactions to authenticated;

drop policy if exists face_video_message_reactions_select_member
    on public.face_video_message_reactions;
create policy face_video_message_reactions_select_member
    on public.face_video_message_reactions
    for select to authenticated
    using (
        face_video_private.face_video_is_chat_member(chat_id)
    );

drop policy if exists face_video_message_reactions_insert_own_member
    on public.face_video_message_reactions;
create policy face_video_message_reactions_insert_own_member
    on public.face_video_message_reactions
    for insert to authenticated
    with check (
        user_id = (select auth.uid())
        and face_video_private.face_video_is_chat_member(chat_id)
    );

drop policy if exists face_video_message_reactions_update_own_member
    on public.face_video_message_reactions;
create policy face_video_message_reactions_update_own_member
    on public.face_video_message_reactions
    for update to authenticated
    using (
        user_id = (select auth.uid())
        and face_video_private.face_video_is_chat_member(chat_id)
    )
    with check (
        user_id = (select auth.uid())
        and face_video_private.face_video_is_chat_member(chat_id)
    );

drop policy if exists face_video_message_reactions_delete_own_member
    on public.face_video_message_reactions;
create policy face_video_message_reactions_delete_own_member
    on public.face_video_message_reactions
    for delete to authenticated
    using (
        user_id = (select auth.uid())
        and face_video_private.face_video_is_chat_member(chat_id)
    );

create or replace function public.face_video_set_message_reaction(
    p_message_id uuid,
    p_chat_id uuid,
    p_reaction text
)
returns boolean
language plpgsql
security invoker
set search_path = ''
as $function$
declare
    v_current_reaction text;
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    if not face_video_private.face_video_is_chat_member(p_chat_id) then
        raise exception 'Chat membership required';
    end if;

    if not exists (
        select 1
          from public.face_video_messages as message
         where message.id = p_message_id
           and message.chat_id = p_chat_id
           and message.deleted_at is null
    ) then
        raise exception 'Message is unavailable for reaction';
    end if;

    if p_reaction is null then
        delete from public.face_video_message_reactions
         where message_id = p_message_id
           and chat_id = p_chat_id
           and user_id = (select auth.uid());
        return false;
    end if;

    if p_reaction not in ('👍', '❤️', '😂', '😮', '😢', '🔥', '👏', '👎') then
        raise exception 'Unsupported message reaction';
    end if;

    select reaction
      into v_current_reaction
      from public.face_video_message_reactions
     where message_id = p_message_id
       and user_id = (select auth.uid());

    if v_current_reaction = p_reaction then
        delete from public.face_video_message_reactions
         where message_id = p_message_id
           and user_id = (select auth.uid());
        return false;
    end if;

    insert into public.face_video_message_reactions (
        message_id,
        chat_id,
        user_id,
        reaction
    )
    values (
        p_message_id,
        p_chat_id,
        (select auth.uid()),
        p_reaction
    )
    on conflict (message_id, user_id)
    do update
       set reaction = excluded.reaction,
           chat_id = excluded.chat_id,
           updated_at = pg_catalog.now();

    return true;
end;
$function$;

revoke all on function public.face_video_set_message_reaction(uuid, uuid, text)
    from public, anon;
grant execute on function public.face_video_set_message_reaction(uuid, uuid, text)
    to authenticated;

do $publication$
begin
    if not exists (
        select 1
          from pg_publication_tables
         where pubname = 'supabase_realtime'
           and schemaname = 'public'
           and tablename = 'face_video_message_reactions'
    ) then
        execute 'alter publication supabase_realtime add table public.face_video_message_reactions';
    end if;
end;
$publication$;
