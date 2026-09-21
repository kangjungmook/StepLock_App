-- 로그인한 계정의 설정과 일별 기록을 보관합니다.
-- 두 테이블 모두 RLS를 켜고 본인 행만 다루게 하므로, 앱에는 공개 키만 둡니다.

create table public.lock_settings (
  user_id uuid primary key references auth.users (id) on delete cascade,
  device_uuid text not null,
  display_name text,
  step_goal integer not null default 8000,
  sleep_goal_hours real not null default 7,
  pomodoro_goal integer not null default 3,
  steps_enabled boolean not null default true,
  sleep_enabled boolean not null default false,
  pomodoro_enabled boolean not null default false,
  require_all_conditions boolean not null default false,
  blocked_app_ids text[] not null default '{}',
  updated_at timestamptz not null default now()
);

create table public.daily_stats (
  user_id uuid not null references auth.users (id) on delete cascade,
  stat_date date not null,
  device_uuid text not null,
  steps integer not null default 0,
  sleep_minutes integer not null default 0,
  pomodoro_sessions integer not null default 0,
  updated_at timestamptz not null default now(),
  primary key (user_id, stat_date)
);

alter table public.lock_settings enable row level security;
alter table public.daily_stats enable row level security;

create policy lock_settings_select_own on public.lock_settings
  for select to authenticated using (auth.uid() = user_id);
create policy lock_settings_insert_own on public.lock_settings
  for insert to authenticated with check (auth.uid() = user_id);
create policy lock_settings_update_own on public.lock_settings
  for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy lock_settings_delete_own on public.lock_settings
  for delete to authenticated using (auth.uid() = user_id);

create policy daily_stats_select_own on public.daily_stats
  for select to authenticated using (auth.uid() = user_id);
create policy daily_stats_insert_own on public.daily_stats
  for insert to authenticated with check (auth.uid() = user_id);
create policy daily_stats_update_own on public.daily_stats
  for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy daily_stats_delete_own on public.daily_stats
  for delete to authenticated using (auth.uid() = user_id);
