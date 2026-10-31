-- Device-specific FCM registration tokens. The mobile client may only
-- manage the tokens belonging to its authenticated user.
create table if not exists public.face_video_push_tokens (
    user_id uuid not null references auth.users(id) on delete cascade,
    token text not null,
    platform text not null default 'android' check (platform in ('android')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint face_video_push_tokens_pkey primary key (user_id, token),
    constraint face_video_push_tokens_token_length check (length(token) between 20 and 4096)
);

alter table public.face_video_push_tokens enable row level security;
revoke all on table public.face_video_push_tokens from anon;
grant select, insert, update, delete on table public.face_video_push_tokens to authenticated;

drop policy if exists "Users can read their own push tokens" on public.face_video_push_tokens;
create policy "Users can read their own push tokens"
on public.face_video_push_tokens for select to authenticated
using ((select auth.uid()) = user_id);

drop policy if exists "Users can register their own push tokens" on public.face_video_push_tokens;
create policy "Users can register their own push tokens"
on public.face_video_push_tokens for insert to authenticated
with check ((select auth.uid()) = user_id);

drop policy if exists "Users can update their own push tokens" on public.face_video_push_tokens;
create policy "Users can update their own push tokens"
on public.face_video_push_tokens for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

drop policy if exists "Users can delete their own push tokens" on public.face_video_push_tokens;
create policy "Users can delete their own push tokens"
on public.face_video_push_tokens for delete to authenticated
using ((select auth.uid()) = user_id);
