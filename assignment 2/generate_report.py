"""Derive the report ONLY from successful Java run files; standard library only."""
from pathlib import Path
from collections import defaultdict
import csv
import html
import math
import os
import re
import shutil
import statistics
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent


def screenshots(task):
    """Capture real browser screenshots of captured stdout, clearly labelled."""
    chrome = next((shutil.which(n) for n in ('google-chrome', 'chromium', 'chromium-browser')
                   if shutil.which(n)), None)
    if chrome is None:
        return 'Chrome unavailable: stdout retained; personal screenshots still needed.'
    lines = (task / 'results/output.txt').read_text(encoding='utf-8-sig').splitlines()
    for page, offset in enumerate(range(0, len(lines), 42), 1):
        chunk = lines[offset:offset + 42]
        title = f'{task.name} - Captured Java execution output ({page})'
        document = '<!doctype html><meta charset="utf-8"><title>' + title + '</title>'
        document += '<style>body{margin:36px;background:#101827;color:#e4edf9;font:17px/1.55 monospace}h1{font:24px sans-serif;color:#81c8ff}p{font:15px sans-serif;color:#afbdd0}pre{white-space:pre-wrap;overflow-wrap:anywhere}</style>'
        document += '<h1>' + title + '</h1><p>Browser-rendered transcript from output.txt; not a local terminal screenshot.</p><pre>'
        document += html.escape('\n'.join(chunk)) + '</pre>'
        path = task / 'results' / f'output-{page:02d}.html'
        path.write_text(document, encoding='utf-8')
        image = path.with_suffix('.png')
        # A fresh profile avoids locks and user account content; the HTML is self-contained.
        with tempfile.TemporaryDirectory() as profile:
            subprocess.run([chrome, '--headless', '--no-sandbox', '--disable-gpu',
                            '--hide-scrollbars', '--no-first-run', '--disable-extensions',
                            f'--user-data-dir={profile}', '--window-size=1600,1550',
                            f'--screenshot={image}', path.as_uri()],
                           check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=45)
        if not image.exists() or image.stat().st_size < 1000:
            raise RuntimeError(f'Screenshot not generated: {image}')
    return 'Browser screenshots of captured stdout generated successfully.'


def main():
    environment = (ROOT / 'environment.txt').read_text(encoding='utf-8-sig')
    lines = [
        '# Assignment 2 (CO2) - Report and measured evidence', '',
        'Repository: https://github.com/UMManishsagar/CSA0916', '',
        '**Student name/register number: not supplied; complete before submission.**', '',
        'This is an AI-assisted implementation and analysis with genuine automated execution evidence. '
        'It does not certify individual student execution or faculty approval. The student must review, '
        'run and explain the work. Task plans were committed before the automated run. ', '',
        '## Execution environment', '', '```text', environment.strip(), '```', '',
        '## Measurement method and limitations', '',
        'Three warmups and seven recorded samples per timed case. Setup is outside System.nanoTime(); '
        'CSV durations cover the complete operation batch. Mean ns/op divides mean batch time by the '
        'reported operation count. SD is sample standard deviation across seven batch durations. '
        'A playlist pair/cycle, a whole report, and a whole sort each count as one labelled operation. '
        'The small educational harness uses one JVM per task, a fixed case order and no isolated '
        'forks; JIT/GC/host contention and shared runner noise can change rankings. Reused String '
        'keys may already have cached hashes. Finite timings support these tested workloads; they '
        'do not prove asymptotic complexity or production tail latency.', '',
        'heap_before/heap_after are raw Runtime occupancy readings WITHOUT forced GC. They cannot '
        'be interpreted as exact allocation or retained-size measurements. Q1 prefix-copy/object '
        'estimates and Q7 instrumented comparison counts provide complementary evidence.', '',
    ]
    all_means = {}
    for task in sorted(ROOT.glob('Task[0-9][0-9]')):
        raw = task / 'results/raw.csv'
        output_path = task / 'results/output.txt'
        if not raw.exists() or not output_path.exists():
            raise RuntimeError(f'Run evidence missing: {task.name}')
        output = output_path.read_text(encoding='utf-8-sig')
        groups = defaultdict(list)
        with raw.open(encoding='utf-8-sig', newline='') as stream:
            for row in csv.DictReader(stream):
                groups[(row['case'], int(row['size']), int(row['operations']))].append(row)
        lines += ['---', '', (task / 'analysis.md').read_text(encoding='utf-8').strip(), '',
                  '### Observed execution evidence', '',
                  f'[Raw samples]({task.name}/results/raw.csv) | '
                  f'[Complete stdout]({task.name}/results/output.txt)', '']
        if groups:
            lines += ['| Case | Size | Ops/sample | Mean batch ms | Median ms | SD ms | Mean ns/op |',
                      '|---|---:|---:|---:|---:|---:|---:|']
            summary = []
            for (name, size, ops), rows in groups.items():
                if len(rows) != 7 or {int(r['repeat']) for r in rows} != set(range(1, 8)):
                    raise RuntimeError(f'Expected seven repetitions: {name}')
                if len({r['checksum'] for r in rows}) != 1:
                    raise RuntimeError(f'Unstable output checksum: {name}')
                ns = [int(r['nanos']) for r in rows]
                mean, median, sd = statistics.mean(ns), statistics.median(ns), statistics.stdev(ns)
                per_op = mean / ops
                if not math.isfinite(per_op) or per_op <= 0:
                    raise RuntimeError(f'Invalid duration: {name}')
                all_means[(task.name, name, size)] = per_op
                lines.append(f'| {name} | {size:,} | {ops:,} | {mean/1e6:.4f} | {median/1e6:.4f} | {sd/1e6:.4f} | {per_op:.2f} |')
                summary.append([name, size, ops, mean, median, sd, per_op])
            with (task / 'results/summary.csv').open('w', encoding='utf-8', newline='') as stream:
                writer = csv.writer(stream)
                writer.writerow(['case', 'size', 'operations', 'mean_ns', 'median_ns', 'sample_sd_ns', 'mean_ns_per_op'])
                writer.writerows(summary)
            lines += ['']
        else:
            lines += ['Q3 uses deterministic correctness experiments; no performance timings are claimed.', '']
        lines += ['```text', output.strip(), '```', '', screenshots(task), '']
        for image in sorted((task / 'results').glob('output-*.png')):
            lines += [f'![{task.name} captured execution transcript]({task.name}/results/{image.name})', '']

    def measured(task, case, size):
        return all_means[(task, case, size)]

    lines += ['## Evidence-based decisions', '', '### Q1 - Ticket construction', '']
    for count, tickets in [(6, 1), (50, 10000), (500, 100)]:
        plus = measured('Task01', f'StringPlus_tickets{tickets}', count)
        builder = measured('Task01', f'StringBuilder_tickets{tickets}', count)
        buffer = measured('Task01', f'StringBuffer_tickets{tickets}', count)
        lines += [f'- {tickets:,} ticket(s), {count} fields: measured plus/builder ratio '
                  f'**{plus/builder:.2f}x**, buffer/builder **{buffer/builder:.2f}x**. '
                  'A ratio above 1 means the numerator took more time.', '']
    lines += ['The character-copy model predicts much larger repeated-prefix cost as fields increase. '
              'The observed ratios above determine whether this run supports the timing prediction; '
              'tiny-ticket differences are particularly noisy. The local-builder recommendation also '
              'rests on linear construction and clear ownership, independent of a tiny-run ranking.', '',
              '### Q2 - Playlist operating boundary', '']
    for op in ['front_pair', 'middle_pair']:
        passing = [n for n in (100, 10000, 100000)
                   if measured('Task02', 'ArrayList_' + op, n) <= 1e6]
        failing = [n for n in (100, 10000, 100000) if n not in passing]
        lines += [f'- ArrayList {op}: within the illustrative 1-ms mean budget at {passing}; '
                  f'outside it at {failing}. No claim is made about untested sizes or p99.', '']
    lines += ['For Up Next choose ArrayDeque for its end-operation API and amortized bounds; '
              'use the measured cycle timings to assess constant factors. ArrayList remains '
              'appropriate for indexed reads and appends; a retained cursor can change the '
              'LinkedList middle-edit decision.', '', '### Q3 - Equality', '',
              'The actual before/after sizes and three predicted-versus-observed fixtures are '
              'printed above. Corrected HashSet, TreeSet and HashMap results are asserted equal; '
              'the Aa/BB collision does not merge unequal students. Mutating hashed identity '
              'breaks lookup in the selected demonstration.', '', '### Q4 - Inventory', '']
    for n in (100, 10000, 100000):
        ns = measured('Task04', 'LinkedHashMap_lookup', n)
        lines += [f'- n={n:,}: LinkedHashMap mean lookup {ns/1000:.3f} microseconds; '
                  f'10-microsecond mean budget **{"met" if ns <= 10000 else "not met"}**.', '']
    lines += ['LinkedHashMap satisfies insertion-order output without a side history index; '
              'sort a copy for occasional code-order reports. The budget results support or limit '
              'its deployment at the tested sizes only. Formatting, comparisons, allocation and '
              'unmeasured I/O remain costs.', '', '### Q5 - Ranking', '']
    for n in (100, 10000, 100000):
        chain = measured('Task05', 'ComparatorChain_sort', n)
        three = measured('Task05', 'ThreeStable_sorts', n)
        lines += [f'- n={n:,}: three-stable-sorts / chain time ratio **{three/chain:.2f}x**.', '']
    lines += ['Use the injected comparator for reusable ranking rules. Correctness tests, not speed, '
              'establish the tie-break and comparator contract on the chosen fixtures.', '',
              '### Q6 - Computational feasibility', '']
    for n in (100, 10000, 100000):
        admit = measured('Task06', 'PriorityQueue_admit', n)
        serve = measured('Task06', 'PriorityQueue_serve', n)
        ms = 5000 * (admit + serve) / 1e6
        lines += [f'- Using n={n:,} batch averages: 5,000 admissions plus 5,000 polls '
                  f'estimate **{ms:.2f} ms** of measured queue work; illustrative 1-second CPU '
                  f'budget **{"met" if ms <= 1000 else "not met"}**.', '']
    lines += ['This is an extrapolation of measured algorithm work, not a hospital load test. '
              'Daily averages do not establish peak concurrency, clinical safety or end-to-end capacity.', '',
              '### Q7 - Final redesign defence', '',
              '| Size | Original mean ms/report | Cold cache ms/report | Warm cache ms/report | Cold speedup | Time objective |',
              '|---|---:|---:|---:|---:|---|']
    for n in (100, 10000, 100000):
        old = measured('Task07', 'Original_sort_each_report', n)
        cold = measured('Task07', 'Redesigned_cold_cache', n)
        hot = measured('Task07', 'Redesigned_warm_cache', n)
        lines += [f'| {n:,} | {old/1e6:.4f} | {cold/1e6:.4f} | {hot/1e6:.4f} | '
                  f'{old/cold:.2f}x | {"met" if cold < old else "not met in this run"} |']
    lines += ['', 'Instrumented assertions verified 20 original sorts versus 1 cold-cache sort '
              'and exactly 20 times as many sorting comparisons for each tested size: a **95% '
              'comparison reduction**, meeting the >=90% work-reduction objective. Actual elapsed '
              'improvement or regression is reported above. Formatting remains linear output work; '
              'the cache adds O(n) persistent references and invalidation complexity. Prefer the '
              'original for infrequent reporting, heavy structural churn or tight memory budgets.', '',
              '## Remaining student submission steps', '',
              'Fill name/register number and deadline, personally execute and review the tasks, '
              'compare reconstructed drivers against the uncropped PPT, add student-terminal '
              'screenshots if faculty requires them, and post the repository link in Classroom. '
              'No Classroom submission or message to Panki has been performed.', '']
    (ROOT / 'REPORT.md').write_text('\n'.join(lines), encoding='utf-8')
    summary_path = os.environ.get('GITHUB_STEP_SUMMARY')
    if summary_path:
        with open(summary_path, 'a', encoding='utf-8') as stream:
            stream.write('# Java assignment evidence\n\nAll seven Java programs completed successfully. '
                         'Seven measured repetitions per timed case and correctness checks passed. '
                         'See assignment 2/REPORT.md and the evidence artifact.\n')
    print('Generated REPORT.md, summary CSV files and transcript screenshots from actual Java results.')


if __name__ == '__main__':
    main()
