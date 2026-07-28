-- 修复 AI 3D 自动保存重复记录。
-- 执行前建议备份数据库。

-- 旧版本保存时 task_id 可能为空：按同一用户、标题、提示词、风格、器型、模型格式分组，只保留最新一条可见记录。
UPDATE yc_ai_model_work older
JOIN yc_ai_model_work newer
  ON older.user_id = newer.user_id
 AND older.status = 1
 AND newer.status = 1
 AND older.task_id IS NULL
 AND newer.task_id IS NULL
 AND older.id < newer.id
 AND COALESCE(older.title, '') = COALESCE(newer.title, '')
 AND COALESCE(older.prompt, '') = COALESCE(newer.prompt, '')
 AND COALESCE(older.style, '') = COALESCE(newer.style, '')
 AND COALESCE(older.vessel, '') = COALESCE(newer.vessel, '')
 AND COALESCE(older.model_format, '') = COALESCE(newer.model_format, '')
SET older.status = 0;

-- 新版本保存会写入 task_id：同一用户、同一腾讯任务只保留最新一条记录，为唯一键清理历史重复数据。
DELETE older
FROM yc_ai_model_work older
JOIN yc_ai_model_work newer
  ON older.user_id = newer.user_id
 AND older.task_id = newer.task_id
 AND older.task_id IS NOT NULL
 AND older.id < newer.id;

ALTER TABLE yc_ai_model_work
  ADD UNIQUE KEY uk_yc_ai_model_work_user_task (user_id, task_id);
