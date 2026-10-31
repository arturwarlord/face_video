
-- Replace inherited broad authenticated table grants with least-privilege grants.
revoke all on table public.face_video_calls from authenticated;
revoke all on table public.face_video_call_signals from authenticated;

grant select, insert on table public.face_video_calls to authenticated;
grant update(status) on table public.face_video_calls to authenticated;

grant select, insert on table public.face_video_call_signals to authenticated;
