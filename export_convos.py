#!/usr/bin/env python3
"""
Export today's Claude Code conversations to readable Markdown files.

Usage:
    python3 export_convos.py                  # exports today's convos
    python3 export_convos.py --date 2026-06-07  # specific date
    python3 export_convos.py --all              # all conversations
    python3 export_convos.py --out ./my-exports # custom output dir
    python3 export_convos.py --project TeamKaidenceHackathon  # filter by project name
"""

import argparse
import json
import os
import sys
from datetime import date, datetime, timezone
from pathlib import Path


def extract_text(content) -> str:
    """Flatten message content (string or block array) to plain text."""
    if isinstance(content, str):
        return content
    if not isinstance(content, list):
        return ""
    parts = []
    for block in content:
        if not isinstance(block, dict):
            continue
        btype = block.get("type", "")
        if btype == "text":
            parts.append(block.get("text", ""))
        elif btype == "tool_use":
            name = block.get("name", "unknown")
            inp = block.get("input", {})
            inp_str = json.dumps(inp, indent=2) if inp else ""
            parts.append(f"[Tool call: {name}]\n```json\n{inp_str}\n```")
        elif btype == "tool_result":
            inner = block.get("content", "")
            if isinstance(inner, list):
                inner = " ".join(
                    b.get("text", "") for b in inner if isinstance(b, dict)
                )
            is_err = block.get("is_error", False)
            label = "Tool error" if is_err else "Tool result"
            parts.append(f"[{label}]\n```\n{str(inner).strip()}\n```")
    return "\n\n".join(p for p in parts if p)


def format_ts(ts_str: str) -> str:
    if not ts_str:
        return ""
    try:
        dt = datetime.fromisoformat(ts_str.replace("Z", "+00:00"))
        return dt.astimezone().strftime("%H:%M:%S")
    except Exception:
        return ts_str


def convo_to_markdown(jsonl_path: Path, project_name: str) -> tuple[str, str, str]:
    """
    Parse a JSONL conversation file and return (title, first_ts, markdown).
    Returns (None, None, None) if the file has no readable messages.
    """
    title = None
    messages = []
    first_ts = None

    with open(jsonl_path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                continue

            obj_type = obj.get("type", "")

            if obj_type == "ai-title" and not title:
                title = obj.get("aiTitle", "")
                continue

            if obj_type not in ("user", "assistant"):
                continue
            if obj.get("isMeta"):
                continue

            msg = obj.get("message", {})
            if not msg:
                continue

            role = msg.get("role", obj_type)
            content = msg.get("content", "")
            ts = obj.get("timestamp", "")

            text = extract_text(content)
            if not text.strip():
                continue

            # Skip slash-command-only messages (e.g. /clear)
            if text.startswith("<command-name>") and len(text) < 300:
                continue

            if not first_ts:
                first_ts = ts

            messages.append((role, ts, text))

    if not messages:
        return None, None, None

    title = title or jsonl_path.stem
    session_id = jsonl_path.stem

    lines = [
        f"# {title}",
        f"",
        f"**Project:** {project_name}  ",
        f"**Session:** `{session_id}`  ",
        f"**Date:** {first_ts[:10] if first_ts else 'unknown'}  ",
        f"",
        "---",
        "",
    ]

    for role, ts, text in messages:
        label = "You" if role == "user" else "Claude"
        time_str = format_ts(ts)
        header = f"### {label}"
        if time_str:
            header += f" _{time_str}_"
        lines.append(header)
        lines.append("")
        lines.append(text.strip())
        lines.append("")
        lines.append("---")
        lines.append("")

    return title, first_ts, "\n".join(lines)


def iter_projects(projects_dir: Path, project_filter: str | None):
    for proj_dir in sorted(projects_dir.iterdir()):
        if not proj_dir.is_dir():
            continue
        name = proj_dir.name
        if project_filter and project_filter.lower() not in name.lower():
            continue
        yield proj_dir, name


def human_project_name(raw: str) -> str:
    """Convert '-Users-foo-bar-MyProject' → 'MyProject'."""
    parts = raw.lstrip("-").split("-")
    # Find the last meaningful segment (after stripping home path noise)
    # The path format is: -Users-<user>-<path...>-<ProjectName>
    # We take the last 1–2 parts as the "display name"
    if len(parts) >= 3:
        return parts[-1]
    return raw


def main():
    parser = argparse.ArgumentParser(description="Export Claude Code conversations to Markdown.")
    parser.add_argument("--date", default=date.today().isoformat(),
                        help="Export conversations from this date (YYYY-MM-DD). Default: today.")
    parser.add_argument("--all", action="store_true",
                        help="Export all conversations regardless of date.")
    parser.add_argument("--out", default="./claude-exports",
                        help="Output directory. Default: ./claude-exports")
    parser.add_argument("--project", default=None,
                        help="Filter by project name (substring match).")
    args = parser.parse_args()

    projects_dir = Path.home() / ".claude" / "projects"
    if not projects_dir.exists():
        print(f"ERROR: {projects_dir} does not exist.", file=sys.stderr)
        sys.exit(1)

    target_date = None if args.all else args.date
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    exported = 0
    skipped = 0

    for proj_dir, raw_name in iter_projects(projects_dir, args.project):
        display_name = human_project_name(raw_name)

        for jsonl in sorted(proj_dir.glob("*.jsonl")):
            # Date filter: use file mtime as a quick pre-filter
            if target_date:
                mtime = datetime.fromtimestamp(jsonl.stat().st_mtime).date().isoformat()
                if mtime != target_date:
                    skipped += 1
                    continue

            title, first_ts, md = convo_to_markdown(jsonl, display_name)
            if not md:
                skipped += 1
                continue

            # Final date check against actual first message timestamp
            if target_date and first_ts and not first_ts.startswith(target_date):
                skipped += 1
                continue

            safe_title = "".join(c if c.isalnum() or c in " -_" else "_" for c in title)[:60].strip()
            filename = f"{display_name}__{safe_title}__{jsonl.stem[:8]}.md"
            out_path = out_dir / filename

            out_path.write_text(md, encoding="utf-8")
            print(f"  Exported: {out_path.name}")
            exported += 1

    print(f"\nDone. {exported} conversation(s) exported to {out_dir}/")
    if skipped:
        print(f"({skipped} file(s) skipped — wrong date or empty)")


if __name__ == "__main__":
    main()
