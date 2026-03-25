FROM mysql:8.0

LABEL maintainer="SQL Judge Engine"
LABEL description="Judge Container for SQL Judge Engine"

# 不预设密码，由 container-manager 在创建容器时通过环境变量传入
ENV MYSQL_DATABASE="judge_db"

# MySQL 8.0 image already includes mysql client, skip apt-get
COPY conf/my.cnf /etc/mysql/conf.d/custom.cnf

COPY docker-entrypoint-initdb.d/* /docker-entrypoint-initdb.d/

EXPOSE 3306

CMD ["mysqld", "--user=mysql"]
