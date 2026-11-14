alter table public.face_video_chat_members
    add column if not exists can_change_info boolean not null default false,
    add column if not exists can_invite_users boolean not null default false,
    add column if not exists can_remove_users boolean not null default false;

update public.face_video_chat_members
   set can_change_info = true,
       can_invite_users = true,
       can_remove_users = true
 where role = 'admin';

create or replace function face_video_private.face_video_can_manage_group_info(p_chat_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
    select (select auth.uid()) is not null
       and exists (
            select 1
            from public.face_video_chat_members cm
            join public.face_video_chats c on c.id = cm.chat_id
            where cm.chat_id = p_chat_id
              and c.is_group = true
              and cm.user_id = (select auth.uid())
              and (
                    cm.role = 'owner'
                    or (cm.role = 'admin' and cm.can_change_info = true)
              )
       );
$function$;

create or replace function face_video_private.face_video_can_invite_group_members(p_chat_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
    select (select auth.uid()) is not null
       and exists (
            select 1
            from public.face_video_chat_members cm
            join public.face_video_chats c on c.id = cm.chat_id
            where cm.chat_id = p_chat_id
              and c.is_group = true
              and cm.user_id = (select auth.uid())
              and (
                    cm.role = 'owner'
                    or (cm.role = 'admin' and cm.can_invite_users = true)
              )
       );
$function$;

create or replace function face_video_private.face_video_can_remove_group_members(p_chat_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
    select (select auth.uid()) is not null
       and exists (
            select 1
            from public.face_video_chat_members cm
            join public.face_video_chats c on c.id = cm.chat_id
            where cm.chat_id = p_chat_id
              and c.is_group = true
              and cm.user_id = (select auth.uid())
              and (
                    cm.role = 'owner'
                    or (cm.role = 'admin' and cm.can_remove_users = true)
              )
       );
$function$;

revoke all on function face_video_private.face_video_can_manage_group_info(uuid) from public, anon;
revoke all on function face_video_private.face_video_can_invite_group_members(uuid) from public, anon;
revoke all on function face_video_private.face_video_can_remove_group_members(uuid) from public, anon;
grant execute on function face_video_private.face_video_can_manage_group_info(uuid) to authenticated;
grant execute on function face_video_private.face_video_can_invite_group_members(uuid) to authenticated;
grant execute on function face_video_private.face_video_can_remove_group_members(uuid) to authenticated;

drop policy if exists face_video_chats_update_by_group_admin on public.face_video_chats;
create policy face_video_chats_update_by_group_permissions
on public.face_video_chats
for update
to authenticated
using (
    is_group = true
    and face_video_private.face_video_can_manage_group_info(id)
)
with check (
    is_group = true
    and face_video_private.face_video_can_manage_group_info(id)
);

drop policy if exists face_video_members_delete_by_self_or_admin on public.face_video_chat_members;
create policy face_video_members_delete_by_permissions
on public.face_video_chat_members
for delete
to authenticated
using (
    (
        user_id = (select auth.uid())
        and role in ('member', 'admin')
    )
    or (
        role = 'member'
        and face_video_private.face_video_can_remove_group_members(chat_id)
    )
    or (
        role = 'admin'
        and face_video_private.face_video_is_chat_owner(chat_id)
    )
);

revoke update (can_change_info, can_invite_users, can_remove_users) on public.face_video_chat_members from anon, authenticated;
grant update (can_change_info, can_invite_users, can_remove_users) on public.face_video_chat_members to authenticated;

create or replace function public.face_video_update_group(
    p_chat_id uuid,
    p_title text,
    p_description text default ''
)
returns boolean
language plpgsql
security invoker
set search_path = ''
as $function$
declare
    v_title text := btrim(coalesce(p_title, ''));
    v_description text := btrim(coalesce(p_description, ''));
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;
    if char_length(v_title) < 2 or char_length(v_title) > 80 then
        raise exception 'Название группы должно содержать от 2 до 80 символов';
    end if;
    if char_length(v_description) > 500 then
        raise exception 'Описание группы должно содержать не более 500 символов';
    end if;
    if not face_video_private.face_video_can_manage_group_info(p_chat_id) then
        raise exception 'У вас нет права изменять информацию группы';
    end if;

    update public.face_video_chats
       set title = v_title,
           description = v_description
     where id = p_chat_id
       and is_group = true;

    if not found then
        raise exception 'Группа не найдена';
    end if;
    return true;
end;
$function$;

create or replace function public.face_video_add_group_members(
    p_chat_id uuid,
    p_member_ids uuid[]
)
returns integer
language plpgsql
security invoker
set search_path = ''
as $function$
declare
    v_member_ids uuid[];
    v_invalid_count integer;
    v_new_count integer;
    v_existing_count integer;
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    select coalesce(array_agg(distinct invited.user_id), array[]::uuid[])
      into v_member_ids
      from unnest(coalesce(p_member_ids, array[]::uuid[])) as invited(user_id)
     where invited.user_id is not null
       and invited.user_id <> (select auth.uid());

    if coalesce(array_length(v_member_ids, 1), 0) = 0 then
        raise exception 'Выбери хотя бы одного друга';
    end if;

    if not face_video_private.face_video_can_invite_group_members(p_chat_id) then
        raise exception 'У вас нет права приглашать участников в эту группу';
    end if;

    select count(*) into v_invalid_count
      from unnest(v_member_ids) as invited(user_id)
     where not exists (
        select 1
          from public.face_video_friendships f
         where f.status = 'accepted'
           and (
                (f.user_low = (select auth.uid()) and f.user_high = invited.user_id)
                or (f.user_high = (select auth.uid()) and f.user_low = invited.user_id)
           )
     );

    if v_invalid_count > 0 then
        raise exception 'Добавлять можно только пользователей из списка друзей';
    end if;

    select count(*) into v_existing_count
      from public.face_video_chat_members cm
     where cm.chat_id = p_chat_id;

    select count(*) into v_new_count
      from unnest(v_member_ids) as invited(user_id)
     where not exists (
        select 1 from public.face_video_chat_members cm
         where cm.chat_id = p_chat_id and cm.user_id = invited.user_id
     );

    if v_existing_count + v_new_count > 50 then
        raise exception 'В группе может быть не более 50 участников';
    end if;

    insert into public.face_video_chat_members (chat_id, user_id, role)
    select p_chat_id, invited.user_id, 'member'
      from unnest(v_member_ids) as invited(user_id)
    on conflict (chat_id, user_id) do nothing;

    get diagnostics v_new_count = row_count;
    return v_new_count;
end;
$function$;

create or replace function public.face_video_set_group_member_role(
    p_chat_id uuid,
    p_target_user_id uuid,
    p_role text
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
    if p_role not in ('admin', 'member') then
        raise exception 'Недопустимая роль участника';
    end if;
    if not face_video_private.face_video_is_chat_owner(p_chat_id) then
        raise exception 'Назначать администраторов может только владелец группы';
    end if;
    if p_target_user_id = (select auth.uid()) then
        raise exception 'Нельзя изменить собственную роль владельца';
    end if;

    update public.face_video_chat_members
       set role = p_role,
           can_change_info = (p_role = 'admin'),
           can_invite_users = (p_role = 'admin'),
           can_remove_users = (p_role = 'admin')
     where chat_id = p_chat_id
       and user_id = p_target_user_id
       and role <> 'owner';

    if not found then
        raise exception 'Участник не найден';
    end if;
    return true;
end;
$function$;

create or replace function public.face_video_set_group_admin_permissions(
    p_chat_id uuid,
    p_target_user_id uuid,
    p_can_change_info boolean,
    p_can_invite_users boolean,
    p_can_remove_users boolean
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
    if not face_video_private.face_video_is_chat_owner(p_chat_id) then
        raise exception 'Настраивать права администраторов может только владелец группы';
    end if;
    if p_target_user_id = (select auth.uid()) then
        raise exception 'Нельзя изменить права владельца';
    end if;

    update public.face_video_chat_members
       set can_change_info = coalesce(p_can_change_info, false),
           can_invite_users = coalesce(p_can_invite_users, false),
           can_remove_users = coalesce(p_can_remove_users, false)
     where chat_id = p_chat_id
       and user_id = p_target_user_id
       and role = 'admin';

    if not found then
        raise exception 'Администратор не найден';
    end if;
    return true;
end;
$function$;

revoke all on function public.face_video_set_group_admin_permissions(uuid, uuid, boolean, boolean, boolean) from public, anon;
grant execute on function public.face_video_set_group_admin_permissions(uuid, uuid, boolean, boolean, boolean) to authenticated;
