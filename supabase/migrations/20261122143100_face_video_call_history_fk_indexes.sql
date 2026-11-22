-- Cover the caller/callee foreign keys used by call-history access and retention.
create index if not exists idx_face_video_call_history_caller
    on public.face_video_call_history(caller_id);

create index if not exists idx_face_video_call_history_callee
    on public.face_video_call_history(callee_id);
