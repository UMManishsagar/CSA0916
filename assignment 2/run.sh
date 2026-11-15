#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p build
{
  date -u '+UTC: %Y-%m-%dT%H:%M:%SZ'
  java -version 2>&1
  javac -version 2>&1
  uname -a
  printf 'Source commit: %s\n' "${GITHUB_SHA:-local-run}"
  printf 'Runner: %s\n' "${RUNNER_NAME:-local-machine}"
  getconf _NPROCESSORS_ONLN || true
  command -v lscpu >/dev/null && lscpu || true
} > environment.txt
sources=(); while IFS= read -r -d '' file; do sources+=("$file"); done < <(find support Task* -name '*.java' -print0)
javac -Xlint:all -d build "${sources[@]}" 2>&1 | tee compilation.txt
classes=(task01.TicketBenchmark task02.PlaylistBenchmark task03.Attendance task04.Inventory task05.RankingDemo task06.Triage task07.Redesign)
for i in "${!classes[@]}"; do
  printf -v task 'Task%02d' "$((i+1))"
  mkdir -p "$task/results"
  java -Xms128m -Xmx1024m -ea -cp build "${classes[$i]}" | tee "$task/results/output.txt"
done
