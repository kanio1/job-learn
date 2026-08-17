-- Live uniqueness after V14. Expect 0 duplicate pairs.
SELECT session_id, kind, COUNT(*) AS copies
  FROM checkout_anomaly
 WHERE session_id IS NOT NULL
 GROUP BY session_id, kind
HAVING COUNT(*) > 1;

SELECT COUNT(*) AS unique_index_present
  FROM pg_indexes
 WHERE schemaname = 'public'
   AND indexname = 'uk_checkout_anomaly_session_kind';

-- Reconstruct "duplicates before cleanup" without touching OLTP.
CREATE TEMP TABLE checkout_anomaly_lab (
    anomaly_id UUID PRIMARY KEY,
    session_id UUID,
    kind       VARCHAR(64) NOT NULL
);

INSERT INTO checkout_anomaly_lab VALUES
    ('00000000-0000-0000-0000-00000000aa01', '00000000-0000-0000-0000-00000000bb01', 'MISSING_FULFILLMENT'),
    ('00000000-0000-0000-0000-00000000aa02', '00000000-0000-0000-0000-00000000bb01', 'MISSING_FULFILLMENT'),
    ('00000000-0000-0000-0000-00000000aa03', '00000000-0000-0000-0000-00000000bb02', 'WRONG_AMOUNT');

SELECT COUNT(*) AS duplicates_before
  FROM (
        SELECT session_id, kind
          FROM checkout_anomaly_lab
         WHERE session_id IS NOT NULL
         GROUP BY session_id, kind
        HAVING COUNT(*) > 1
       ) d;

DELETE FROM checkout_anomaly_lab a
      USING checkout_anomaly_lab b
 WHERE a.anomaly_id > b.anomaly_id
   AND a.session_id IS NOT NULL
   AND a.session_id = b.session_id
   AND a.kind = b.kind;

SELECT COUNT(*) AS rows_after FROM checkout_anomaly_lab;

SELECT COUNT(*) AS duplicates_after
  FROM (
        SELECT session_id, kind
          FROM checkout_anomaly_lab
         WHERE session_id IS NOT NULL
         GROUP BY session_id, kind
        HAVING COUNT(*) > 1
       ) d;
