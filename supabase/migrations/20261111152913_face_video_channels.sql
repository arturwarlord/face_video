create table public.face_video_channels (
    id uuid primary key default gen_random_uuid(),
    title text not null check (char_length(btrim(title)) between 2 and 80),
    description text not null default '' check (char_length(description) <= 500),
    created_by uuid not null references auth.users(id) on delete cascade,
    is_public boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index face_video_channels_created_at_idx
    on public.face_video_channels (created_at desc);

create index face_video_channels_created_by_idx
    on public.face_video_channels (created_by);

create table public.face_video_channel_members (
    channel_id uuid not null references public.face_video_channels(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    role text not null default 'subscriber'
        check (role in ('owner', 'admin', 'subscriber')),
    joined_at timestamptz not null default now(),
    primary key (channel_id, user_id)
);

create index face_video_channel_members_user_idx
    on public.face_video_channel_members (user_id, joined_at desc);

create table public.face_video_channel_posts (
    id uuid primary key default gen_random_uuid(),
    channel_id uuid not null references public.face_video_channels(id) on delete cascade,
    author_id uuid not null references auth.users(id) on delete cascade,
    body text not null check (char_length(btrim(body)) between 1 and 4000),
    created_at timestamptz not null default now(),
    edited_at timestamptz
);

create index face_video_channel_posts_timeline_idx
    on public.face_video_channel_posts (channel_id, created_at desc);

alter table public.face_video_channels enable row level security;
alter table public.face_video_channel_members enable row level security;
alter table public.face_video_channel_posts enable row level security;

create policy face_video_channels_read_public_or_owner
on public.face_video_channels
for select
to authenticated
using (is_public = true or created_by = (select auth.uid()));

create policy face_video_channels_insert_own
on public.face_video_channels
for insert
to authenticated
with check (created_by = (select auth.uid()));

create policy face_video_channels_update_own
on public.face_video_channels
for update
to authenticated
using (created_by = (select auth.uid()))
with check (created_by = (select auth.uid()));

create policy face_video_channels_delete_own
on public.face_video_channels
for delete
to authenticated
using (created_by = (select auth.uid()));

create policy face_video_channel_members_read_self
on public.face_video_channel_members
for select
to authenticated
using (user_id = (select auth.uid()));

create policy face_video_channel_members_join_public
on public.face_video_channel_members
for insert
to authenticated
with check (
    user_id = (select auth.uid())
    and (
        (
            role = 'subscriber'
            and exists (
                select 1
                from public.face_video_channels c
                where c.id = channel_id
                  and c.is_public = true
            )
        )
        or
        (
            role = 'owner'
            and exists (
                select 1
                from public.face_video_channels c
                where c.id = channel_id
                  and c.created_by = (select auth.uid())
            )
        )
    )
);

create policy face_video_channel_members_leave_self
on public.face_video_channel_members
for delete
to authenticated
using (user_id = (select auth.uid()) and role = 'subscriber');

create policy face_video_channel_posts_read_visible_channels
on public.face_video_channel_posts
for select
to authenticated
using (
    exists (
        select 1
        from public.face_video_channels c
        where c.id = channel_id
          and (c.is_public = true or c.created_by = (select auth.uid()))
    )
);

create policy face_video_channel_posts_insert_owner
on public.face_video_channel_posts
for insert
to authenticated
with check (
    author_id = (select auth.uid())
    and exists (
        select 1
        from public.face_video_channels c
        where c.id = channel_id
          and c.created_by = (select auth.uid())
    )
);

create policy face_video_channel_posts_delete_owner
on public.face_video_channel_posts
for delete
to authenticated
using (
    author_id = (select auth.uid())
    and exists (
        select 1
        from public.face_video_channels c
        where c.id = channel_id
          and c.created_by = (select auth.uid())
    )
);

grant select, insert, update, delete on public.face_video_channels to authenticated;
grant select, insert, delete on public.face_video_channel_members to authenticated;
grant select, insert, delete on public.face_video_channel_posts to authenticated;

create or replace function public.face_video_create_channel(
    p_title text,
    p_description text default '',
    p_is_public boolean default true
)
returns uuid
language plpgsql
security invoker
set search_path to 'public', 'pg_temp'
as $function$
declare
    v_channel_id uuid;
    v_title text := btrim(coalesce(p_title, ''));
    v_description text := btrim(coalesce(p_description, ''));
begin
    if (select auth.uid()) is null then
        raise exception 'Authentication required';
    end if;

    if char_length(v_title) < 2 or char_length(v_title) > 80 then
        raise exception 'Название канала должно содержать от 2 до 80 символов';
    end if;

    if char_length(v_description) > 500 then
        raise exception 'Описание канала должно содержать не более 500 символов';
    end if;

    insert into public.face_video_channels (title, description, created_by, is_public)
    values (v_title, v_description, (select auth.uid()), coalesce(p_is_public, true))
    returning id into v_channel_id;

    insert into public.face_video_channel_members (channel_id, user_id, role)
    values (v_channel_id, (select auth.uid()), 'owner');

    return v_channel_id;
end;
$function$;

revoke all on function public.face_video_create_channel(text, text, boolean) from public, anon;
grant execute on function public.face_video_create_channel(text, text, boolean) to authenticated;
