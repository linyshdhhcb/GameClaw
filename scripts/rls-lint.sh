#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GAMECLAW_DIR="$(dirname "$SCRIPT_DIR")"

SQL_DIR="$GAMECLAW_DIR/base/src/main/resources/db/migration"

if [ ! -d "$SQL_DIR" ]; then
    echo "✅ No migration directory found, skipping RLS lint"
    exit 0
fi

errors=0

for sql_file in "$SQL_DIR"/V*.sql; do
    [ -f "$sql_file" ] || continue

    tables=$(grep -oiE 'CREATE\s+TABLE\s+(\w+)' "$sql_file" | awk '{print $NF}' || true)
    if [ -z "$tables" ]; then
        continue
    fi

    for table in $tables; do
        if grep -qiE "ALTER\s+TABLE\s+${table}\s+ENABLE\s+ROW\s+LEVEL\s+SECURITY" "$SQL_DIR"/V*.sql; then
            echo "✅ Table '$table' has RLS enabled"
        else
            echo "❌ Table '$table' is missing ENABLE ROW LEVEL SECURITY (defined in $sql_file)"
            errors=$((errors + 1))
        fi
    done
done

if [ "$errors" -gt 0 ]; then
    echo ""
    echo "❌ RLS lint failed: $errors table(s) missing RLS policy"
    echo "   Every CREATE TABLE must have a corresponding ALTER TABLE ... ENABLE ROW LEVEL SECURITY"
    exit 1
fi

echo "✅ RLS lint passed: all tables have RLS enabled"
