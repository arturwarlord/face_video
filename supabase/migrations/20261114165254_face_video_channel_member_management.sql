alter table public.face_video_channel_members
    add column if not exists can_change_info boolean not null default false,
    add column if not exists can_invite_users boolean not null default false,
    add column if not exists can_remove_users boolean not null default false,
    add column if not exists can_publish_posts boolean not null default false;

update public.face_video_channel_members
   set can_change_info = true,
       can_invite_users = true,
       can_remove_users = true,
       can_publish_posts = true
 where role = 'admin';

create or replace function face_video_private.face_video_is_channel_owner(p_channel_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
    select (select auth.uid()) is not null
       and exists (
            select 1
            from public.face_video_channels c
            where c.id = p_channel_id
              and c.created_by = (select auth.uid())
       );
$function$;

create or replace function face_video_private.face_video_channel_has_permission(
    p_channel_id uuid,
    p_permission text
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
    select (select auth.uid()) is not null
       and (
            face_video_private.face_video_is_channel_owner(p_channel_id)
            or exists (
                select 1
                from public.face_video_channel_members cm
                where cm.channel_id = p_channel_id
                  and cm.user_id = (select auth.uid())
                  and cm.role = 'admin'
                  and case p_permission
                        when 'change_info' then cm.can_change_info
                        when 'invite_users' then cm.can_invite_users
                        when 'remove_users' then cm.can_remove_users
                        when 'publish_posts' then cm.can_publish_posts
                        when 'view_members' then cm.can_invite_users or cm.can_remove_users
                        else false
                      end
            )
       );
$function$;

revoke all on function face_video_private.face_video_is_channel_owner(uuid) from public, anon;
revoke all on function face_video_private.face_video_channel_has_permission(uuid, text) from public, anon;
grant execute on function face_video_private.face_video_is_channel_owner(uuid) to authenticated;
grant execute on function face_video_private.face_video_channel_has_permission(uuid, text) to authenticated;

drop policy if exists face_video_channel_members_read_channel_owner on public.face_video_channel_members;
create policy face_video_channel_members_read_managers
on public.face_video_channel_members
for select
to authenticated
using (
    user_id = (select auth.uid())
    or face_video_private.face_video_channel_has_permission(channel_id, 'view_members')
);

drop policy if exists face_video_channel_members_join_public on public.face_video_channel_members;
create policy face_video_channel_members_join_or_invite
on public.face_video_channel_members
for insert
to authenticated
with check (
    (
        user_id = (select auth.uid())
        and role = 'subscriber'
        and exists (
            select 1 from public.face_video_channels c
            where c.id = channel_id
              and c.is_public = true
        )
    )
    or (
        user_id = (select auth.uid())
        and role = 'owner'
        and face_video_private.face_video_is_channel_owner(channel_id)
    )
    or (
        user_id <> (select auth.uid())
        and role = 'subscriber'
        and face_video_private.face_video_channel_has_permission(channel_id, 'invite_users')
        and exists (
            select 1
            from public.face_video_friendships f
            where f.status = 'accepted'
              and (
                    (f.user_low = (select auth.uid()) and f.user_high = user_id)
                    or (f.user_high = (select auth.uid()) and f.user_low = user_id)
              )
        )
    )
);

drop policy if exists face_video_channel_members_leave_self on public.face_video_channel_members;
create policy face_video_channel_members_leave_self
on public.face_video_channel_members
for delete
to authenticated
using (
    user_id = (select auth.uid())
    and role = 'subscriber'
);

drop policy if exists face_video_channel_members_remove_by_manager on public.face_video_channel_members;
create policy face_video_channel_members_remove_by_manager
on public.face_video_channel_members
for delete
to authenticated
using (
    (
        role = 'subscriber'
        and face_video_private.face_video_channel_has_permission(channel_id, 'remove_users')
    )
    or (
        role = 'admin'
        and face_video_private.face_video_is_channel_owner(channel_id)
    )
);

drop policy if exists face_video_channel_members_update_role_by_owner on public.face_video_channel_members;
create policy face_video_channel_members_update_role_by_owner
on public.face_video_channel_members
for update
to authenticated
using (
    face_video_private.face_video_is_channel_owner(channel_id)
)
with check (
    face_video_private.face_video_is_channel_owner(channel_id)
    and (
        (user_id = (select auth.uid()) and role = 'owner')
        or (user_id <> (select auth.uid()) and role in ('admin', 'subscriber'))
    )
    and (
        role = 'admin'
        or (
            can_change_info = false
            and can_invite_users = false
            and can_remove_users = false
            and can_publish_posts = false
        )
    )
);

revoke update on public.face_video_channel_members from authenticated;
grant update (role, can_change_info, can_invite_users, can_remove_users, can_publish_posts)
    on public.face_video_channel_members to authenticated;

drop policy if exists face_video_channels_update_own on public.face_video_channels;
create policy face_video_channels_update_by_permissions
on public.face_video_channels
for update
to authenticated
using (
    face_video_private.face_video_channel_has_permission(id, 'change_info')
)
with check (
    face_video_private.face_video_channel_has_permission(id, 'change_info')
);

revoke update on public.face_video_channels from authenticated;
grant update (title, description, updated_at, avatar_url)
    on public.face_video_channels to authenticated;

drop policy if exists face_video_channel_posts_insert_owner on public.face_video_channel_posts;
create policy face_video_channel_posts_insert_by_publish_permission
on public.face_video_channel_posts
for insert
to authenticated
with check (
    author_id = (select auth.uid())
    and face_video_private.face_video_channel_has_permission(channel_id, 'publish_posts')
);

drop policy if exists face_video_channel_posts_update_owner on public.face_video_channel_posts;
create policy face_video_channel_posts_update_by_author_permission
on public.face_video_channel_posts
for update
to authenticated
using (
    author_id = (select auth.uid())
    and face_video_private.face_video_channel_has_permission(channel_id, 'publish_posts')
)
with check (
    author_id = (select auth.uid())
    and face_video_private.face_video_channel_has_permission(channel_id, 'publish_posts')
);

drop policy if exists face_video_channel_posts_delete_owner on public.face_video_channel_posts;
create policy face_video_channel_posts_delete_by_author_permission
on public.face_video_channel_posts
for delete
to authenticated
using (
    author_id = (select auth.uid())
    and face_video_private.face_video_channel_has_permission(channel_id, 'publish_posts')
);

create or replace function public.face_video_add_channel_members(
    p_channel_id uuid,
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
    v_existing_count integer;
    v_new_count integer;
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    if not face_video_private.face_video_channel_has_permission(p_channel_id, 'invite_users') then
        raise exception 'У вас нет права приглашать участников в этот канал';
    end if;

    select coalesce(array_agg(distinct invited.user_id), array[]::uuid[])
      into v_member_ids
      from unnest(coalesce(p_member_ids, array[]::uuid[])) as invited(user_id)
     where invited.user_id is not null
       and invited.user_id <> (select auth.uid());

    if coalesce(array_length(v_member_ids, 1), 0) = 0 then
        raise exception 'Выбери хотя бы одного друга';
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
      from public.face_video_channel_members cm
     where cm.channel_id = p_channel_id;

    select count(*) into v_new_count
      from unnest(v_member_ids) as invited(user_id)
     where not exists (
        select 1
        from public.face_video_channel_members cm
        where cm.channel_id = p_channel_id
          and cm.user_id = invited.user_id
     );

    if v_existing_count + v_new_count > 500 then
        raise exception 'В канале может быть не более 500 участников';
    end if;

    insert into public.face_video_channel_members (channel_id, user_id, role)
    select p_channel_id, invited.user_id, 'subscriber'
      from unnest(v_member_ids) as invited(user_id)
    on conflict (channel_id, user_id) do nothing;

    get diagnostics v_new_count = row_count;
    return v_new_count;
end;
$function$;

create or replace function public.face_video_set_channel_member_role(
    p_channel_id uuid,
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
    if p_role not in ('admin', 'subscriber') then
        raise exception 'Недопустимая роль участника канала';
    end if;
    if not face_video_private.face_video_is_channel_owner(p_channel_id) then
        raise exception 'Назначать администраторов канала может только владелец';
    end if;
    if p_target_user_id = (select auth.uid()) then
        raise exception 'Нельзя изменить собственную роль владельца';
    end if;

    update public.face_video_channel_members
       set role = p_role,
           can_change_info = (p_role = 'admin'),
           can_invite_users = (p_role = 'admin'),
           can_remove_users = (p_role = 'admin'),
           can_publish_posts = (p_role = 'admin')
     where channel_id = p_channel_id
       and user_id = p_target_user_id
       and role in ('admin', 'subscriber');

    if not found then
        raise exception 'Участник канала не найден';
    end if;
    return true;
end;
$function$;

create or replace function public.face_video_set_channel_admin_permissions(
    p_channel_id uuid,
    p_target_user_id uuid,
    p_can_change_info boolean,
    p_can_invite_users boolean,
    p_can_remove_users boolean,
    p_can_publish_posts boolean
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
    if not face_video_private.face_video_is_channel_owner(p_channel_id) then
        raise exception 'Настраивать права администраторов канала может только владелец';
    end if;
    if p_target_user_id = (select auth.uid()) then
        raise exception 'Нельзя изменить права владельца';
    end if;

    update public.face_video_channel_members
       set can_change_info = coalesce(p_can_change_info, false),
           can_invite_users = coalesce(p_can_invite_users, false),
           can_remove_users = coalesce(p_can_remove_users, false),
           can_publish_posts = coalesce(p_can_publish_posts, false)
     where channel_id = p_channel_id
       and user_id = p_target_user_id
       and role = 'admin';

    if not found then
        raise exception 'Администратор канала не найден';
    end if;
    return true;
end;
$function$;

create or replace function public.face_video_remove_channel_member(
    p_channel_id uuid,
    p_target_user_id uuid
)
returns boolean
language plpgsql
security invoker
set search_path = ''
as $function$
declare
    v_role text;
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;
    if p_target_user_id = (select auth.uid()) then
        raise exception 'Чтобы покинуть канал, используй действие «Отписаться»';
    end if;

    select cm.role into v_role
      from public.face_video_channel_members cm
     where cm.channel_id = p_channel_id
       and cm.user_id = p_target_user_id;

    if v_role is null then
        raise exception 'Участник канала не найден';
    end if;

    if v_role = 'admin' then
        if not face_video_private.face_video_is_channel_owner(p_channel_id) then
            raise exception 'Удалять администраторов канала может только владелец';
        end if;
    elsif v_role = 'subscriber' then
        if not face_video_private.face_video_channel_has_permission(p_channel_id, 'remove_users') then
            raise exception 'У вас нет права удалять подписчиков';
        end if;
    else
        raise exception 'Нельзя удалить владельца канала';
    end if;

    delete from public.face_video_channel_members
     where channel_id = p_channel_id
       and user_id = p_target_user_id;

    return found;
end;
$function$;

revoke all on function public.face_video_add_channel_members(uuid, uuid[]) from public, anon;
revoke all on function public.face_video_set_channel_member_role(uuid, uuid, text) from public, anon;
revoke all on function public.face_video_set_channel_admin_permissions(uuid, uuid, boolean, boolean, boolean, boolean) from public, anon;
revoke all on function public.face_video_remove_channel_member(uuid, uuid) from public, anon;

grant execute on function public.face_video_add_channel_members(uuid, uuid[]) to authenticated;
grant execute on function public.face_video_set_channel_member_role(uuid, uuid, text) to authenticated;
grant execute on function public.face_video_set_channel_admin_permissions(uuid, uuid, boolean, boolean, boolean, boolean) to authenticated;
grant execute on function public.face_video_remove_channel_member(uuid, uuid) to authenticated;
