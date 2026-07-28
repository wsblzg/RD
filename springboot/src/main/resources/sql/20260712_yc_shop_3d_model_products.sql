-- 窑创未来 文创商城 3D 模型商品增量脚本
-- 日期: 2026-07-12
-- 说明:
-- 1) 当前代码复用 yc_shop_product / yc_shop_order / yc_shop_order_item，不需要新增表。
-- 2) 完整 GLB 只在订单经后台管理员审核为 PAID/SHIPPED 后由接口返回。
-- 3) 3D 商品用商品编码/名称/详情元信息识别；后续若要更规范，可再加 product_type/model_url 字段。
-- 4) 执行前请替换下方 OSS 示例地址。

SET NAMES utf8mb4;

USE `wyxm_ycwl`;

-- 可选索引：加速“已审核支付成功的订单 -> 已购 3D 模型”查询。
-- MySQL 5.7 没有 CREATE INDEX IF NOT EXISTS，用动态 SQL 避免重复执行报错。
SET @index_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'yc_shop_order_item'
    AND index_name = 'idx_yc_shop_order_item_product_order'
);

SET @ddl := IF(
  @index_exists = 0,
  'CREATE INDEX idx_yc_shop_order_item_product_order ON yc_shop_order_item (product_id, order_id)',
  'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 示例：新增/更新一个可售卖的 GLB 模型商品。
-- 识别规则：
-- - product_code / name / subtitle / detail_content 包含 GLB、3D、模型、modelUrl、glbUrl、模型地址等关键词时，会进入商城“3D 模型”板块。
-- - 正视图会展示给所有用户。
-- - modelUrl/模型地址会在未购买时从接口返回中剥离；管理员审核支付成功后才返回 modelUrl。
--
-- 请替换：
-- - https://your-bucket.oss-cn-xxx.aliyuncs.com/models/ceramic-vase/front.webp
-- - https://your-bucket.oss-cn-xxx.aliyuncs.com/models/ceramic-vase/side.webp
-- - https://your-bucket.oss-cn-xxx.aliyuncs.com/models/ceramic-vase/top.webp
-- - https://your-bucket.oss-cn-xxx.aliyuncs.com/models/ceramic-vase/model.glb

INSERT INTO `yc_shop_product`
(`product_code`, `name`, `subtitle`, `cover_url`, `detail_content`, `price`, `stock`, `sold_count`, `is_on_shelf`, `status`, `sort_no`, `created_by`, `updated_by`)
VALUES
(
  'GLB-CERAMIC-001',
  '柴烧花器 3D 模型',
  '商城售卖 GLB 数字模型，未购买仅展示正视图',
  'https://your-bucket.oss-cn-xxx.aliyuncs.com/models/ceramic-vase/front.webp',
  CONCAT(
    '柴烧花器数字模型，可用于课程展示、作品陈列与浏览器 3D 展示。', CHAR(10),
    'front: https://your-bucket.oss-cn-xxx.aliyuncs.com/models/ceramic-vase/front.webp', CHAR(10),
    'modelUrl: https://your-bucket.oss-cn-xxx.aliyuncs.com/models/ceramic-vase/model.glb'
  ),
  29.90,
  999,
  0,
  1,
  1,
  80,
  NULL,
  NULL
)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `subtitle` = VALUES(`subtitle`),
  `cover_url` = VALUES(`cover_url`),
  `detail_content` = VALUES(`detail_content`),
  `price` = VALUES(`price`),
  `stock` = VALUES(`stock`),
  `is_on_shelf` = VALUES(`is_on_shelf`),
  `status` = VALUES(`status`),
  `sort_no` = VALUES(`sort_no`),
  `updated_by` = VALUES(`updated_by`),
  `updated_at` = NOW();
