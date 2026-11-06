-- Prefer invoker RPCs and enforce every friendship transition through RLS.
grant select, insert, update, delete on public.face_video_friendships to authenticated;

drop policy if exists "face_video_friendships_insert_request" on public.face_video_friendships;
create policy "face_video_friendships_insert_request"
on public.face_video_friendships for insert to authenticated
with check (
    requested_by = (select auth.uid())
    and status = 'pending'
    and (user_low = (select auth.uid()) or user_high = (select auth.uid()))
);

drop policy if exists "face_video_friendships_accept_incoming" on public.face_video_friendships;
create policy "face_video_friendships_accept_incoming"
on public.face_video_friendships for update to authenticated
using (
    status = 'pending'
    and requested_by <> (select auth.uid())
    and (user_low = (select auth.uid()) or user_high = (select auth.uid()))
)
with check (
    status = 'accepted'
    and requested_by <> (select auth.uid())
    and (user_low = (select auth.uid()) or user_high = (select auth.uid()))
);

drop policy if exists "face_video_friendships_delete_participant" on public.face_video_friendships;
create policy "face_video_friendships_delete_participant"
on public.face_video_friendships for delete to authenticated
using (user_low = (select auth.uid()) or user_high = (select auth.uid()));

create or replace function face_video_private.face_video_guard_friendship_update()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.id <> old.id
       or new.user_low <> old.user_low
       or new.user_high <> old.user_high
       or new.requested_by <> old.requested_by
       or new.created_at <> old.created_at then
        raise exception 'Friendship identity fields are immutable';
    end if;
    if old.status <> 'pending' or new.status <> 'accepted' then
        raise exception 'Only an incoming pending request can be accepted';
    end if;
    new.updated_at := now();
    return new;
end;
$$;

drop trigger if exists face_video_friendships_guard_update on public.face_video_friendships;
create trigger face_video_friendships_guard_update
before update on public.face_video_friendships
for each row execute function face_video_private.face_video_guard_friendship_update();

alter function public.face_video_send_friend_request(uuid) security invoker;
alter function public.face_video_respond_friend_request(uuid, boolean) security invoker;
alter function public.face_video_cancel_friend_request(uuid) security invoker;
alter function public.face_video_remove_friend(uuid) security invoker;

revoke all on function public.face_video_send_friend_request(uuid) from public, anon;
revoke all on function public.face_video_respond_friend_request(uuid, boolean) from public, anon;
revoke all on function public.face_video_cancel_friend_request(uuid) from public, anon;
revoke all on function public.face_video_remove_friend(uuid) from public, anon;
grant execute on function public.face_video_send_friend_request(uuid) to authenticated;
grant execute on function public.face_video_respond_friend_request(uuid, boolean) to authenticated;
grant execute on function public.face_video_cancel_friend_request(uuid) to authenticated;
grant execute on function public.face_video_remove_friend(uuid) to authenticated;
