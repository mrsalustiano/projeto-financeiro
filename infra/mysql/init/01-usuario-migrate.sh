#!/usr/bin/env bash
set -euo pipefail

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<EOSQL
CREATE USER IF NOT EXISTS 'fincontrol_migrate'@'%' IDENTIFIED BY '${MYSQL_MIGRATE_PASSWORD}';
GRANT ALL PRIVILEGES ON fincontrol.* TO 'fincontrol_migrate'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EOSQL
