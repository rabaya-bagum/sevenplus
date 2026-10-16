# Online challenges (Supabase)

Without setup, challenges work **offline on one phone**. To let friends join each other's
challenges, connect a free Supabase project:

1. Create a project at supabase.com → **SQL Editor** → run:

```sql
create table public.challenges (
  id               text primary key,
  code             text not null unique,
  title            text not null,
  target           int  not null check (target between 1 and 365),
  start            timestamptz not null,
  "end"            timestamptz not null,
  participants     jsonb not null default '[]',
  participant_ids  text[] not null default '{}'
);
create index challenges_participant_ids on public.challenges using gin (participant_ids);

alter table public.challenges enable row level security;
-- Prototype policy: anyone with the app can read and update challenges.
-- Before a public launch, add Sign in with Apple (Supabase Auth) and restrict
-- updates to rows whose participant_ids contain auth.uid().
create policy "read"   on public.challenges for select using (true);
create policy "insert" on public.challenges for insert with check (true);
create policy "update" on public.challenges for update using (true);
```

2. **Project Settings → API**: copy the Project URL and the `anon` public key.
3. In Xcode, app target → **Info** tab, add two String keys:
   - `SupabaseURL` = `https://YOUR-PROJECT.supabase.co`
   - `SupabaseAnonKey` = the anon key
4. Run the app. Progress → Challenges now shows **Join a friend** and invite codes.

Notes
- The anon key is meant to be public; security comes from the row-level policies above.
- The prototype policies are open on purpose so you can test quickly. Tighten them before
  launch as described in the SQL comment.
