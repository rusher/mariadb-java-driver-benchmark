CREATE USER 'perf'@'%' IDENTIFIED BY '!Password0';
GRANT ALL ON *.* TO 'perf'@'%' IDENTIFIED BY '!Password0';
GRANT SUPER ON *.* TO 'perf'@'%';

FLUSH PRIVILEGES;
