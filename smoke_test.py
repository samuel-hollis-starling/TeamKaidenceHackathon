#!/usr/bin/env python3
"""
Smoke test: load attendees, book them all, run assignment, print scores.
Usage: python3 smoke_test.py [--base-url http://localhost:8080]
"""

import argparse
import json
import sys
import time
import urllib.request
import urllib.error
from pathlib import Path

BASE_URL = "http://localhost:8080"
ATTENDEES_FILE = Path(__file__).parent / "input-data" / "har-attendees-2026-06-03.json"


def post(url: str, body: dict) -> dict:
    data = json.dumps(body).encode()
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())


def get(url: str) -> dict:
    with urllib.request.urlopen(url) as resp:
        return json.loads(resp.read())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default=BASE_URL)
    args = parser.parse_args()
    base = args.base_url.rstrip("/")

    attendees = json.loads(ATTENDEES_FILE.read_text())
    print(f"Loaded {len(attendees)} attendees from {ATTENDEES_FILE.name}")

    # --- 1. POST bookings ---
    print(f"\nPOSTing bookings to {base}/api/bookings ...")
    failures = []
    for i, person in enumerate(attendees, 1):
        payload = {
            "employeeId": person["id"],
            "socialPreference": "NONE",
            "feelingLucky": False,
        }
        try:
            post(f"{base}/api/bookings", payload)
            print(f"  [{i:>2}/{len(attendees)}] {person['name']}")
        except urllib.error.URLError as e:
            print(f"  [{i:>2}/{len(attendees)}] FAILED {person['name']}: {e}")
            failures.append(person["id"])

    if failures:
        print(f"\nWARN: {len(failures)} bookings failed: {failures}")

    # --- 2. GET bookings ---
    print(f"\nGET {base}/api/bookings ...")
    bookings = get(f"{base}/api/bookings")
    booked = bookings.get("bookings", [])
    print(f"  Server has {len(booked)} bookings confirmed.")

    # --- 3. POST assignments/run ---
    print(f"\nPOST {base}/api/assignments/run  (SA — this takes a few seconds) ...")
    t0 = time.time()
    assignments = post(f"{base}/api/assignments/run", {})
    elapsed = time.time() - t0
    assigned = assignments.get("deskByEmployeeId", {})
    print(f"  Done in {elapsed:.1f}s — {len(assigned)} employees assigned.")

    # --- 4. GET assignments/score ---
    print(f"\nGET {base}/api/assignments/score ...")
    score = get(f"{base}/api/assignments/score")

    print("\n" + "=" * 50)
    print("  ASSIGNMENT SCORE REPORT")
    print("=" * 50)
    for key, val in score.items():
        label = key.replace("_", " ").title()
        if isinstance(val, float):
            print(f"  {label:<30} {val:>10.4f}")
        else:
            print(f"  {label:<30} {val!s:>10}")
    print("=" * 50)


if __name__ == "__main__":
    main()
