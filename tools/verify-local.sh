#!/usr/bin/env bash
set -eu

project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_root"

verify_dir="$project_root/qa/verify"
rm -rf "$verify_dir"
mkdir -p "$verify_dir/server-core" "$verify_dir/csv-core"

java -m jdk.compiler/com.sun.tools.javac.Main -d "$verify_dir/server-core" \
  $(find server/src/main/java -name '*.java' ! -path '*/servlet/*' | sort)

java -m jdk.compiler/com.sun.tools.javac.Main -cp "$verify_dir/server-core" \
  -d "$verify_dir/server-core" tools/CoreSelfTest.java
java -cp "$verify_dir/server-core" CoreSelfTest

java -m jdk.compiler/com.sun.tools.javac.Main -d "$verify_dir/csv-core" \
  client/src/main/java/ua/edu/nupp/servetra/client/model/Models.java \
  client/src/main/java/ua/edu/nupp/servetra/client/importer/SemicolonCsv.java \
  client/src/main/java/ua/edu/nupp/servetra/client/importer/EquipmentCsvMapper.java \
  tools/CsvSelfTest.java
java -cp "$verify_dir/csv-core" CsvSelfTest

if rg -n 'java\.sql|javax\.sql|jdbc:' client/src/main/java; then
  echo 'FAIL: JDBC reference found in the client' >&2
  exit 1
fi

if rg -n 'org\.springframework|org\.hibernate|jakarta\.servlet' server client; then
  echo 'FAIL: prohibited framework/API reference found' >&2
  exit 1
fi

uk_keys="$verify_dir/uk.keys"
en_keys="$verify_dir/en.keys"
cut -d= -f1 client/src/main/resources/i18n/messages_uk.properties | sort > "$uk_keys"
cut -d= -f1 client/src/main/resources/i18n/messages_en.properties | sort > "$en_keys"
cmp "$uk_keys" "$en_keys"

echo "Java files: $(find . -type f -name '*.java' ! -path './qa/*' | wc -l)"
echo "JUnit @Test methods: $(rg -g '*.java' '@Test' server/src/test client/src/test | wc -l)"
echo "PreparedStatement references in DAO: $(rg -g '*.java' 'PreparedStatement' server/src/main/java/ua/edu/nupp/servetra/server/dao | wc -l)"
echo 'Static and dependency-free executable checks passed.'
