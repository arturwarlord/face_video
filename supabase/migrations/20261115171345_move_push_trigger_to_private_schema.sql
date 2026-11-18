-- Keep the trigger implementation outside the exposed public API schema.
create or replace function private.notify_arturchat_push_from_insert()
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

revoke all on function private.notify_arturchat_push_from_insert() from public, anon;
grant execute on function private.notify_arturchat_push_from_insert() to authenticated, service_role;

drop trigger if exists arturchat_push_messages_insert on public.face_video_messages;
create trigger arturchat_push_messages_insert
after insert on public.face_video_messages
for each row execute function private.notify_arturchat_push_from_insert();

drop trigger if exists arturchat_push_calls_insert on public.face_video_calls;
create trigger arturchat_push_calls_insert
after insert on public.face_video_calls
for each row execute function private.notify_arturchat_push_from_insert();

revoke all on function public.notify_arturchat_push_from_insert() from public, anon, authenticated, service_role;
drop function public.notify_arturchat_push_from_insert();
