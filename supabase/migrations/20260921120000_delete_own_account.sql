-- 계정 삭제. Play 스토어는 계정을 만들 수 있는 앱에 앱 안의 삭제 경로를 요구합니다.
--
-- auth.users 는 앱의 공개 키로는 지울 수 없으므로, 소유자 권한으로 도는
-- security definer 함수를 두고 호출자 본인(auth.uid())만 지우게 합니다.
-- lock_settings · daily_stats 는 auth.users 를 on delete cascade 로 참조하므로
-- 이 한 번의 삭제로 계정의 모든 행이 함께 사라집니다.
--
-- Edge Function 과 service_role 키를 쓰는 방법도 있지만, 키를 따로 보관하고
-- 함수를 별도 배포해야 해서 움직이는 부분이 늘어납니다.

create or replace function public.delete_own_account()
returns void
language plpgsql
security definer
-- search_path 를 고정하지 않으면 security definer 함수가 탈취될 수 있습니다.
set search_path = ''
as $$
begin
  if auth.uid() is null then
    raise exception 'not authenticated';
  end if;

  delete from auth.users where id = auth.uid();
end;
$$;

-- 로그인한 사용자만 호출할 수 있게 합니다.
revoke all on function public.delete_own_account() from public, anon;
grant execute on function public.delete_own_account() to authenticated;
