#!/usr/bin/env bash
set -euo pipefail

root="${MYSQL_ROOT_PASSWORD//$'\r'/}"
migrate="${MYSQL_MIGRATE_PASSWORD//$'\r'/}"

mysql --protocol=socket -uroot -p"${root}" <<EOSQL
CREATE USER IF NOT EXISTS 'fincontrol_migrate'@'%' IDENTIFIED BY '${migrate}';
GRANT ALL PRIVILEGES ON fincontrol.* TO 'fincontrol_migrate'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EOSQL
