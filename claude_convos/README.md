# Claude Conversation Exporter

Exports Claude Code conversation history to readable Markdown files.

## Usage

```bash
# Export today's conversations (default output: ./claude-exports/)
python3 claude_convos/export_convos.py

# Export to a specific folder
python3 claude_convos/export_convos.py --out conversations/

# Specific date
python3 claude_convos/export_convos.py --date 2026-06-07

# All conversations ever
python3 claude_convos/export_convos.py --all

# Filter to a specific project
python3 claude_convos/export_convos.py --project TeamKaidenceHackathon

# Combine flags
python3 claude_convos/export_convos.py --date 2026-06-08 --out conversations/ --project TeamKaidenceHackathon
```

## Output

Each conversation becomes a Markdown file named:

```
<ProjectName>__<Conversation Title>__<session-id-prefix>.md
```

The file contains every user and Claude turn with timestamps. Tool calls are shown as fenced JSON blocks.

## Requirements

Python 3 standard library only — no pip installs needed.

## How it works

Claude Code stores conversation history as JSONL files under `~/.claude/projects/`. Each line is a message event (user turn, assistant turn, tool call, etc.). The script filters to the target date, extracts human-readable text from each turn, and writes a Markdown file per session.
