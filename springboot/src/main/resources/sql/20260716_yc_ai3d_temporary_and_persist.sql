-- AI 3D 临时作品与主动永久保存增量脚本。
-- 执行前请备份数据库；本脚本应在 20260712_yc_ai3d_dedupe_task_id.sql 之后执行。

ALTER TABLE yc_ai_model_work
  MODIFY COLUMN model_url TEXT NULL COMMENT '生成完成后的临时模型地址',
  MODIFY COLUMN cover_url TEXT NULL COMMENT '作品预览图地址',
  MODIFY COLUMN oss_url TEXT NULL COMMENT '永久保存后的模型地址',
  ADD COLUMN generation_status VARCHAR(16) NOT NULL DEFAULT 'READY' COMMENT 'SUBMITTED/RUNNING/READY/FAILED' AFTER oss_url,
  ADD COLUMN storage_status VARCHAR(24) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/TEMPORARY/PERSISTING/PERMANENT/PERSIST_FAILED' AFTER generation_status,
  ADD COLUMN generated_at DATETIME NULL COMMENT '生成完成时间' AFTER storage_status,
  ADD COLUMN expires_at DATETIME NULL COMMENT '临时模型失效时间' AFTER generated_at,
  ADD COLUMN persist_started_at DATETIME NULL COMMENT '开始永久保存时间' AFTER expires_at,
  ADD COLUMN persisted_at DATETIME NULL COMMENT '永久保存完成时间' AFTER persist_started_at,
  ADD COLUMN generation_points_cost INT NOT NULL DEFAULT 10 COMMENT '生成消耗积分' AFTER persisted_at,
  ADD COLUMN persist_points_cost INT NOT NULL DEFAULT 10 COMMENT '永久保存消耗积分' AFTER generation_points_cost,
  ADD COLUMN generation_charge_status VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/CHARGED/REFUNDED/FREE' AFTER persist_points_cost,
  ADD COLUMN persist_charge_status VARCHAR(16) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/CHARGED/REFUNDED/FREE' AFTER generation_charge_status,
  ADD COLUMN model_size_bytes BIGINT NULL COMMENT '永久模型文件大小' AFTER persist_charge_status,
  ADD COLUMN last_error VARCHAR(1000) NULL COMMENT '最近一次处理失败原因' AFTER model_size_bytes,
  ADD INDEX idx_yc_ai_work_user_generation (user_id, generation_status, created_at),
  ADD INDEX idx_yc_ai_work_user_storage (user_id, storage_status, generated_at),
  ADD INDEX idx_yc_ai_work_user_expires (user_id, expires_at);

-- 已经有永久地址的历史作品按永久作品迁移，不追溯扣费。
UPDATE yc_ai_model_work
SET generation_status = 'READY',
    storage_status = 'PERMANENT',
    generated_at = COALESCE(generated_at, created_at),
    persisted_at = COALESCE(persisted_at, created_at),
    generation_charge_status = 'FREE',
    persist_charge_status = 'FREE'
WHERE status = 1
  AND oss_url IS NOT NULL
  AND oss_url <> '';

-- 没有永久地址的历史作品仅按旧临时记录保留，超过24小时后查询时自动隐藏。
UPDATE yc_ai_model_work
SET generation_status = 'READY',
    storage_status = 'TEMPORARY',
    generated_at = COALESCE(generated_at, created_at),
    expires_at = COALESCE(expires_at, DATE_ADD(created_at, INTERVAL 24 HOUR)),
    generation_charge_status = 'FREE',
    persist_charge_status = 'NONE'
WHERE status = 1
  AND (oss_url IS NULL OR oss_url = '');
