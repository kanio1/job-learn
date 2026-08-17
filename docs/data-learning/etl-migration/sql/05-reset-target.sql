DELETE FROM learning_dwh.fact_payment;
DELETE FROM learning_staging.payment;
DELETE FROM learning_etl.batch_run WHERE pipeline_name = 'payment-etl';
