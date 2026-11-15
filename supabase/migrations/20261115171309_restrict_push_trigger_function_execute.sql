-- Remove direct RPC execution of the public SECURITY DEFINER trigger function.
revoke execute on function public.notify_arturchat_push_from_insert() from authenticated;
