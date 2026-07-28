-- 窑创未来 积分充值与 AI 3D 消耗增量脚本
-- 执行前请先备份数据库。

ALTER TABLE yc_user_account
  ADD COLUMN points_balance INT NOT NULL DEFAULT 0 COMMENT '当前可用积分',
  ADD COLUMN points_total_recharged INT NOT NULL DEFAULT 0 COMMENT '累计充值获得积分',
  ADD COLUMN points_total_spent INT NOT NULL DEFAULT 0 COMMENT '累计消耗积分',
  ADD COLUMN points_is_unlimited TINYINT(1) NOT NULL DEFAULT 0 COMMENT '积分是否无限';

UPDATE yc_user_account
SET points_is_unlimited = 1
WHERE username = 'ycadmin' OR role = 'admin';

CREATE TABLE IF NOT EXISTS yc_points_recharge_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  recharge_no VARCHAR(64) NOT NULL UNIQUE COMMENT '充值单号',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  amount DECIMAL(10,2) NOT NULL COMMENT '付款金额',
  points_amount INT NOT NULL COMMENT '到账积分',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT 'PENDING_PAYMENT/PENDING_REVIEW/APPROVED/REJECTED',
  payment_marked_at DATETIME NULL COMMENT '用户标记已付款时间',
  payment_reviewed_at DATETIME NULL COMMENT '管理员审核时间',
  payment_review_by BIGINT NULL COMMENT '审核管理员ID',
  payment_review_remark VARCHAR(255) NULL COMMENT '审核备注',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_yc_points_recharge_user (user_id, created_at),
  INDEX idx_yc_points_recharge_status (status, updated_at),
  INDEX idx_yc_points_recharge_review_by (payment_review_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分充值审核单';

-- 已执行过 3D 模型示例商品脚本时，可把三视图元信息改成单正视图。
UPDATE yc_shop_product
SET detail_content = REPLACE(
  detail_content,
  '三视图: https://your-bucket.oss-cn-xxx.aliyuncs.com/models/ceramic-vase/front.webp,https://your-bucket.oss-cn-xxx.aliyuncs.com/models/ceramic-vase/side.webp,https://your-bucket.oss-cn-xxx.aliyuncs.com/models/ceramic-vase/top.webp',
  'front: https://your-bucket.oss-cn-xxx.aliyuncs.com/models/ceramic-vase/front.webp'
)
WHERE detail_content LIKE '%threeViewImages:%'
   OR detail_content LIKE '%三视图:%';
