-- Friend relationships are represented by one normalized row per user pair.
create table if not exists public.face_video_friendships (
    id uuid primary key default gen_random_uuid(),
    user_low uuid not null references auth.users(id) on delete cascade,
    user_high uuid not null references auth.users(id) on delete cascade,
    requested_by uuid not null references auth.users(id) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'accepted')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint face_video_friendships_pair_order check (user_low < user_high),
    constraint face_video_friendships_requester_in_pair check (requested_by in (user_low, user_high)),
    constraint face_video_friendships_unique_pair unique (user_low, user_high)
);

create index if not exists idx_face_video_friendships_low_status
    on public.face_video_friendships(user_low, status);
create index if not exists idx_face_video_friendships_high_status
    on public.face_video_friendships(user_high, status);
create index if not exists idx_face_video_friendships_requested_by_status
    on public.face_video_friendships(requested_by, status);

alter table public.face_video_friendships enable row level security;

drop policy if exists "face_video_friendships_read_participants" on public.face_video_friendships;
create policy "face_video_friendships_read_participants"
on public.face_video_friendships for select to authenticated
using (
    (select auth.uid()) = user_low
    or (select auth.uid()) = user_high
);

revoke all on public.face_video_friendships from anon, authenticated;
grant select on public.face_video_friendships to authenticated;

create or replace function public.face_video_send_friend_request(p_target_user_id uuid)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_low uuid;
    v_high uuid;
    v_requested_by uuid;
    v_status text;
begin
    if v_actor is null then raise exception 'Authentication required'; end if;
    if p_target_user_id is null or p_target_user_id = v_actor then raise exception 'Cannot add yourself as a friend'; end if;
    if not exists (select 1 from public.face_video_profiles p where p.id = p_target_user_id) then
        raise exception 'ArturChat user not found';
    end if;
    v_low := least(v_actor, p_target_user_id);
    v_high := greatest(v_actor, p_target_user_id);
    insert into public.face_video_friendships(user_low, user_high, requested_by, status)
    values (v_low, v_high, v_actor, 'pending')
    on conflict (user_low, user_high) do nothing;
    select f.requested_by, f.status into v_requested_by, v_status
      from public.face_video_friendships f
     where f.user_low = v_low and f.user_high = v_high
     for update;
    if v_status = 'accepted' then return 'accepted'; end if;
    if v_requested_by <> v_actor then
        update public.face_video_friendships set status = 'accepted', updated_at = now()
         where user_low = v_low and user_high = v_high;
        return 'accepted';
    end if;
    return 'pending_sent';
end;
$$;

create or replace function public.face_video_respond_friend_request(p_target_user_id uuid, p_accept boolean)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_low uuid;
    v_high uuid;
    v_requester uuid;
begin
    if v_actor is null then raise exception 'Authentication required'; end if;
    if p_target_user_id is null or p_target_user_id = v_actor then raise exception 'Invalid friend request'; end if;
    v_low := least(v_actor, p_target_user_id);
    v_high := greatest(v_actor, p_target_user_id);
    select f.requested_by into v_requester
      from public.face_video_friendships f
     where f.user_low = v_low and f.user_high = v_high
       and f.status = 'pending' and f.requested_by = p_target_user_id
     for update;
    if not found then raise exception 'Incoming friend request not found'; end if;
    if coalesce(p_accept, false) then
        update public.face_video_friendships set status = 'accepted', updated_at = now()
         where user_low = v_low and user_high = v_high;
        return 'accepted';
    else
        delete from public.face_video_friendships where user_low = v_low and user_high = v_high;
        return 'declined';
    end if;
end;
$$;

create or replace function public.face_video_cancel_friend_request(p_target_user_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_low uuid;
    v_high uuid;
begin
    if v_actor is null then raise exception 'Authentication required'; end if;
    if p_target_user_id is null or p_target_user_id = v_actor then raise exception 'Invalid friend request'; end if;
    v_low := least(v_actor, p_target_user_id);
    v_high := greatest(v_actor, p_target_user_id);
    delete from public.face_video_friendships
     where user_low = v_low and user_high = v_high
       and status = 'pending' and requested_by = v_actor;
    return found;
end;
$$;

create or replace function public.face_video_remove_friend(p_target_user_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor uuid := (select auth.uid());
    v_low uuid;
    v_high uuid;
begin
    if v_actor is null then raise exception 'Authentication required'; end if;
    if p_target_user_id is null or p_target_user_id = v_actor then raise exception 'Invalid friend'; end if;
    v_low := least(v_actor, p_target_user_id);
    v_high := greatest(v_actor, p_target_user_id);
    delete from public.face_video_friendships
     where user_low = v_low and user_high = v_high and status = 'accepted';
    return found;
end;
$$;

revoke all on function public.face_video_send_friend_request(uuid) from public, anon;
revoke all on function public.face_video_respond_friend_request(uuid, boolean) from public, anon;
revoke all on function public.face_video_cancel_friend_request(uuid) from public, anon;
revoke all on function public.face_video_remove_friend(uuid) from public, anon;
grant execute on function public.face_video_send_friend_request(uuid) to authenticated;
grant execute on function public.face_video_respond_friend_request(uuid, boolean) to authenticated;
grant execute on function public.face_video_cancel_friend_request(uuid) to authenticated;
grant execute on function public.face_video_remove_friend(uuid) to authenticated;
