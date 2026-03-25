-- 创建 judge 用户（判题专用）
-- 注意：此脚本在容器初始化时由 MySQL 官方镜像自动执行
-- 实际生产中，judge 用户的密码由容器管理器动态生成和管理

-- 创建 judge 用户（如果不存在）
-- 密码将在容器启动时由应用程序设置
CREATE USER IF NOT EXISTS 'judge'@'%' IDENTIFIED BY 'temp_password';

-- 授予权限：只能操作 problem_ 开头的数据库
GRANT ALL PRIVILEGES ON `problem_%`.* TO 'judge'@'%';

-- 禁止 GRANT 权限（防止提权）
REVOKE GRANT OPTION ON `problem_%`.* FROM 'judge'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 设置默认数据库
USE mysql;

-- 确保只允许本地连接（可选，增强安全性）
-- ALTER USER 'judge'@'%' REQUIRE SSL;
