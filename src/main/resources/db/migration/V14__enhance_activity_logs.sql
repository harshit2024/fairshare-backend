ALTER TABLE activity_logs
ADD COLUMN ip_address INET;

ALTER TABLE activity_logs
ADD COLUMN user_agent TEXT;