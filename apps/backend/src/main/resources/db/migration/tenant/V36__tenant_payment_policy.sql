ALTER TABLE tenants
    ADD COLUMN payment_policy JSONB NOT NULL DEFAULT '{
        "autoCapture": false,
        "maxAutoCaptureMinor": 0,
        "riskThreshold": 50,
        "refundPolicy": "MANUAL"
    }'::jsonb;
