-- Make call and signaling tables accessible only to authenticated users.
revoke all on table public.face_video_calls from anon, public;
revoke all on table public.face_video_call_signals from anon, public;

grant select, insert on table public.face_video_calls to authenticated;
grant update(status) on table public.face_video_calls to authenticated;

grant select, insert on table public.face_video_call_signals to authenticated;
