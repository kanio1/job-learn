-- Runtime contract after V18 (V17 history is left as-is).
SELECT c.relforcerowsecurity
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'public'
   AND c.relname = 'rls_lab_item';

SELECT polname, pg_get_expr(polqual, polrelid) AS using_expr
  FROM pg_policy
 WHERE polrelid = 'public.rls_lab_item'::regclass;

SELECT rolname, rolbypassrls
  FROM pg_roles
 WHERE rolname IN ('rls_lab_app', 'rls_lab_bypass')
 ORDER BY rolname;
