-- 窑创未来创客大赛测试账号
-- 账号：yctest1、yctest2、yctest3
-- 初始密码：yc1433223
-- 每个账号初始 20 积分；脚本可重复执行，重复执行会重置这三个账号的测试状态。

INSERT INTO yc_user_account
    (username, password_hash, display_name, role, status,
     points_balance, points_total_recharged, points_total_spent, points_is_unlimited)
VALUES
    ('yctest1', '$2a$10$yL/POizQAuonae6usnNtv.xa5eXvjRLGhye1ssm3cAbkOIFEaTr0K', '创客测试用户1', 'user', 1, 20, 20, 0, 0),
    ('yctest2', '$2a$10$lKeNtIOs1RDzJ3stiFFVFuNM0tbyXt2kv2Eej5fqZX8.sUy1dppxK', '创客测试用户2', 'user', 1, 20, 20, 0, 0),
    ('yctest3', '$2a$10$/KwbYk2YJiDlEfS03cub/eQsdl/zqANZOIpwnwGbqVgwgJ2CP9sV6', '创客测试用户3', 'user', 1, 20, 20, 0, 0)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    display_name = VALUES(display_name),
    role = 'user',
    status = 1,
    points_balance = 20,
    points_total_recharged = 20,
    points_total_spent = 0,
    points_is_unlimited = 0,
    updated_at = NOW();
