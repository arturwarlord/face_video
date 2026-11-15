-- Trigger functions are invoked by table triggers; authenticated clients do not need direct EXECUTE access.
revoke execute on function private.notify_arturchat_push_from_insert() from public, anon, authenticated;
