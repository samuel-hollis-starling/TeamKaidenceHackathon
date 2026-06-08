#!/usr/bin/env python3
"""
Desk-booking simulation CLI.

Commands:
  list  <name>   Show everyone in an org subtree (dry-run)
  book  <name>   Book everyone in subtree then run SA + print score
  reset          Clear all bookings on the server
  score          Print current assignment score (no bookings)
  smoke          Book all attendees from har-attendees file, run SA (original smoke test)

Examples:
  python3 sim.py list "Raman Bhatia"
  python3 sim.py list "Raman Bhatia" --depth 2
  python3 sim.py book "Raman Bhatia" --window-rate 0.3 --lucky-rate 0.1
  python3 sim.py book "Engineering" --max 40 --social TALK_TO_ME
  python3 sim.py score
  python3 sim.py smoke
"""

import argparse
import json
import random
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

BASE_URL = "http://localhost:8080"
ORGCHART_FILE = Path(__file__).parent / "input-data" / "orgchart.json"
ATTENDEES_FILE = Path(__file__).parent / "input-data" / "har-attendees-2026-06-03.json"

SOCIAL_CHOICES = ["NONE", "TALK_TO_ME", "DONT_TALK_TO_ME"]


# ---------------------------------------------------------------------------
# HTTP helpers
# ---------------------------------------------------------------------------

def get(base: str, path: str) -> dict | list:
    with urllib.request.urlopen(f"{base}{path}") as resp:
        return json.loads(resp.read())


def post(base: str, path: str, body: dict, timeout: int = 30) -> dict:
    data = json.dumps(body).encode()
    req = urllib.request.Request(
        f"{base}{path}", data=data, headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read())


def delete(base: str, path: str) -> dict:
    req = urllib.request.Request(f"{base}{path}", method="DELETE")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


# ---------------------------------------------------------------------------
# Org chart helpers
# ---------------------------------------------------------------------------

def load_orgchart() -> dict:
    return json.loads(ORGCHART_FILE.read_text())


def find_node(orgchart: dict, name: str) -> dict | None:
    """Case-insensitive partial match on the 'name' field."""
    needle = name.lower()
    # Prefer exact match first
    for node in orgchart.values():
        if node["name"].lower() == needle:
            return node
    # Fall back to substring
    matches = [n for n in orgchart.values() if needle in n["name"].lower()]
    if len(matches) == 1:
        return matches[0]
    if len(matches) > 1:
        names = ", ".join(m["name"] for m in matches)
        print(f"Ambiguous name '{name}' — matches: {names}", file=sys.stderr)
        sys.exit(1)
    return None


def subtree(orgchart: dict, root_id: str, max_depth: int | None) -> list[dict]:
    """BFS walk from root, returning all nodes (including root) up to max_depth."""
    result = []
    queue = [(root_id, 0)]
    while queue:
        node_id, depth = queue.pop(0)
        node = orgchart.get(node_id)
        if node is None:
            continue
        result.append(node)
        if max_depth is None or depth < max_depth:
            for child_id in node.get("children", []):
                queue.append((child_id, depth + 1))
    return result


# ---------------------------------------------------------------------------
# Booking helpers
# ---------------------------------------------------------------------------

def desk_count(base: str) -> int:
    desks = get(base, "/api/desks")
    return len(desks)


def already_booked_ids(base: str) -> set[str]:
    data = get(base, "/api/bookings")
    return {b["employeeId"] for b in data.get("bookings", [])}


def make_payload(emp_id: str, social: str, feeling_lucky: bool) -> dict:
    return {
        "employeeId": emp_id,
        "socialPreference": social,
        "feelingLucky": feeling_lucky,
    }


def book_employees(
    base: str,
    employees: list[dict],
    social: str,
    lucky_rate: float,
    skip_existing: bool,
) -> tuple[int, int]:
    """POST bookings. Returns (booked, skipped)."""
    existing = already_booked_ids(base) if skip_existing else set()
    booked = skipped = 0
    for i, emp in enumerate(employees, 1):
        emp_id = emp["id"]
        if emp_id in existing:
            print(f"  [{i:>{len(str(len(employees)))}}/{len(employees)}] SKIP (already booked)  {emp['name']}")
            skipped += 1
            continue
        feeling_lucky = random.random() < lucky_rate
        payload = make_payload(emp_id, social, feeling_lucky)
        try:
            post(base, "/api/bookings", payload)
            flags = []
            if feeling_lucky:
                flags.append("lucky")
            if social != "NONE":
                flags.append(social.lower())
            flag_str = f"  [{', '.join(flags)}]" if flags else ""
            print(f"  [{i:>{len(str(len(employees)))}}/{len(employees)}] {emp['name']}{flag_str}")
            booked += 1
        except urllib.error.URLError as e:
            print(f"  [{i:>{len(str(len(employees)))}}/{len(employees)}] FAILED  {emp['name']}: {e}")
    return booked, skipped


def print_score(base: str):
    score = get(base, "/api/assignments/score")
    print("\n" + "=" * 52)
    print("  ASSIGNMENT SCORE REPORT")
    print("=" * 52)
    for key, val in score.items():
        label = key.replace("_", " ").title()
        if isinstance(val, float):
            print(f"  {label:<34} {val:>10.4f}")
        else:
            print(f"  {label:<34} {val!s:>10}")
    print("=" * 52)


def run_assignment(base: str) -> tuple[dict, float]:
    print(f"\nPOST {base}/api/assignments/run  (SA — a few seconds) …")
    t0 = time.time()
    result = post(base, "/api/assignments/run", {}, timeout=120)
    elapsed = time.time() - t0
    assigned = result.get("deskByEmployeeId", {})
    print(f"  Done in {elapsed:.1f}s — {len(assigned)} employees assigned.")
    return result, elapsed


# ---------------------------------------------------------------------------
# Commands
# ---------------------------------------------------------------------------

def cmd_list(args):
    orgchart = load_orgchart()
    root = find_node(orgchart, args.name)
    if root is None:
        print(f"No employee found matching '{args.name}'", file=sys.stderr)
        sys.exit(1)

    nodes = subtree(orgchart, root["id"], args.depth)
    print(f"Subtree rooted at '{root['name']}' ({root['role'] or root['org'] or '—'}) — {len(nodes)} people")
    if args.depth is not None:
        print(f"  (depth limit: {args.depth})")
    print()

    # Group by depth for display
    by_depth: dict[int, list] = {}
    for node in nodes:
        d = len(node["orgPath"]) - len(root["orgPath"])
        by_depth.setdefault(d, []).append(node)

    for depth in sorted(by_depth):
        indent = "  " * depth
        for node in by_depth[depth]:
            role = node.get("role") or node.get("org") or ""
            print(f"  {indent}{node['name']}  ({role})")


def cmd_book(args):
    orgchart = load_orgchart()
    root = find_node(orgchart, args.name)
    if root is None:
        print(f"No employee found matching '{args.name}'", file=sys.stderr)
        sys.exit(1)

    nodes = subtree(orgchart, root["id"], args.depth)
    print(f"Subtree: '{root['name']}' — {len(nodes)} people in tree")

    n_desks = desk_count(args.base_url)
    print(f"Desks available: {n_desks}")

    cap = args.max if args.max else n_desks
    if len(nodes) > cap:
        print(f"Sampling {cap} of {len(nodes)} (random)")
        nodes = random.sample(nodes, cap)

    print(f"\nPOSTing {len(nodes)} bookings …")
    booked, skipped = book_employees(
        args.base_url, nodes,
        social=args.social,
        lucky_rate=args.lucky_rate,
        skip_existing=args.skip_existing,
    )
    print(f"\n  Booked: {booked}  |  Skipped: {skipped}")

    _, _ = run_assignment(args.base_url)
    print_score(args.base_url)


def cmd_reset(args):
    result = delete(args.base_url, "/api/bookings")
    remaining = len(result.get("bookings", []))
    print(f"Bookings cleared. Remaining: {remaining}")


def cmd_score(args):
    data = get(args.base_url, "/api/bookings")
    booked = data.get("bookings", [])
    print(f"Current bookings: {len(booked)}")
    print_score(args.base_url)


def cmd_smoke(args):
    if not ATTENDEES_FILE.exists():
        print(f"Attendees file not found: {ATTENDEES_FILE}", file=sys.stderr)
        sys.exit(1)

    attendees = json.loads(ATTENDEES_FILE.read_text())
    print(f"Loaded {len(attendees)} attendees from {ATTENDEES_FILE.name}")

    existing = already_booked_ids(args.base_url) if args.skip_existing else set()
    failures = []
    booked = skipped = 0

    print(f"\nPOSTing bookings to {args.base_url}/api/bookings …")
    for i, person in enumerate(attendees, 1):
        if person["id"] in existing:
            print(f"  [{i:>3}/{len(attendees)}] SKIP  {person['name']}")
            skipped += 1
            continue
        payload = make_payload(person["id"], "NONE", False, False)
        try:
            post(args.base_url, "/api/bookings", payload)
            print(f"  [{i:>3}/{len(attendees)}] {person['name']}")
            booked += 1
        except urllib.error.URLError as e:
            print(f"  [{i:>3}/{len(attendees)}] FAILED  {person['name']}: {e}")
            failures.append(person["id"])

    if failures:
        print(f"\nWARN: {len(failures)} failures: {failures}")
    print(f"\n  Booked: {booked}  |  Skipped: {skipped}")

    _, _ = run_assignment(args.base_url)
    print_score(args.base_url)


# ---------------------------------------------------------------------------
# Argument parsing
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Desk-booking simulation CLI",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--base-url", default=BASE_URL, metavar="URL")
    sub = parser.add_subparsers(dest="command", required=True)

    # -- list --
    p_list = sub.add_parser("list", help="Show org subtree members (dry-run)")
    p_list.add_argument("name", help="Name of subtree root (partial match OK)")
    p_list.add_argument("--depth", type=int, default=None, metavar="N",
                        help="Max depth below root (default: unlimited)")

    # -- book --
    p_book = sub.add_parser("book", help="Book org subtree and run assignment")
    p_book.add_argument("name", help="Name of subtree root (partial match OK)")
    p_book.add_argument("--depth", type=int, default=None, metavar="N",
                        help="Max depth below root (default: unlimited)")
    p_book.add_argument("--max", type=int, default=None, metavar="N",
                        help="Cap at N people (random sample); default: cap at desk count")
    p_book.add_argument("--social", choices=SOCIAL_CHOICES, default="NONE",
                        help="Social preference applied to everyone (default: NONE)")
    p_book.add_argument("--lucky-rate", type=float, default=0.0, metavar="F",
                        help="Fraction feeling lucky, 0–1 (default: 0)")
    p_book.add_argument("--skip-existing", action="store_true",
                        help="Skip employees already booked (instead of re-posting)")

    # -- reset --
    sub.add_parser("reset", help="Clear all bookings on the server")

    # -- score --
    sub.add_parser("score", help="Print current assignment score")

    # -- smoke --
    p_smoke = sub.add_parser("smoke", help="Book all har-attendees and run assignment")
    p_smoke.add_argument("--skip-existing", action="store_true",
                         help="Skip employees already booked")

    args = parser.parse_args()
    args.base_url = args.base_url.rstrip("/")

    dispatch = {
        "list": cmd_list,
        "book": cmd_book,
        "reset": cmd_reset,
        "score": cmd_score,
        "smoke": cmd_smoke,
    }
    dispatch[args.command](args)


if __name__ == "__main__":
    main()
