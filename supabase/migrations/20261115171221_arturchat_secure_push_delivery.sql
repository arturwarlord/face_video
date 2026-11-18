-- Secure push delivery for new ArturChat messages and incoming calls.
-- pg_net and Supabase Vault provide asynchronous HTTP delivery and encrypted secret storage.

create schema if not exists private;

do $$
declare
  secret_count integer;
begin
  select count(*) into secret_count
  from vault.decrypted_secrets
  where name = 'arturchat_push_webhook_secret_v1';

  if secret_count = 0 then
    perform vault.create_secret(
      encode(extensions.gen_random_bytes(32), 'hex'),
      'arturchat_push_webhook_secret_v1',
      'Private authentication key for ArturChat push triggers',
      null::uuid
    );
  elsif secret_count <> 1 then
    raise exception 'Expected exactly one ArturChat push Vault secret, found %', secret_count;
  end if;
end
$$;

create or replace function public.verify_arturchat_push_secret(p_supplied_secret text)
returns boolean
language sql
security definer
set search_path = ''
as $function$
  select exists (
    select 1
    from vault.decrypted_secrets
    where name = 'arturchat_push_webhook_secret_v1'
      and decrypted_secret = p_supplied_secret
  );
$function$;

revoke all on function public.verify_arturchat_push_secret(text) from public, anon, authenticated;
grant execute on function public.verify_arturchat_push_secret(text) to service_role;

create or replace function public.notify_arturchat_push_from_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
declare
  webhook_secret text;
  webhook_payload jsonb;
begin
  select decrypted_secret
    into webhook_secret
  from vault.decrypted_secrets
  where name = 'arturchat_push_webhook_secret_v1';

  if webhook_secret is null then
    raise warning 'ArturChat push Vault secret is missing; skipping notification';
    return new;
  end if;

  webhook_payload := jsonb_build_object(
    'type', 'INSERT',
    'table', tg_table_name,
    'schema', tg_table_schema,
    'record', to_jsonb(new),
    'old_record', null
  );

  perform net.http_post(
    url := 'https://mitvjkhlsvxcnzhbiwye.supabase.co/functions/v1/send-arturchat-push',
    body := webhook_payload,
    params := '{}'::jsonb,
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'x-arturchat-push-secret', webhook_secret
    ),
    timeout_milliseconds := 5000
  );

  return new;
exception
  when others then
    raise warning 'ArturChat push trigger failed without blocking the database write: %', sqlerrm;
    return new;
end;
$function$;

revoke all on function public.notify_arturchat_push_from_insert() from public, anon;
grant execute on function public.notify_arturchat_push_from_insert() to authenticated, service_role;

drop trigger if exists arturchat_push_messages_insert on public.face_video_messages;
create trigger arturchat_push_messages_insert
after insert on public.face_video_messages
for each row execute function public.notify_arturchat_push_from_insert();

drop trigger if exists arturchat_push_calls_insert on public.face_video_calls;
create trigger arturchat_push_calls_insert
after insert on public.face_video_calls
for each row execute function public.notify_arturchat_push_from_insert();
