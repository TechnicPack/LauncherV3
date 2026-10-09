"""Validate curated release boundaries before any promotion side effects."""

import json
import re
import sys
from pathlib import Path


def section(content, name):
    match = re.search(
        r"^## \[" + re.escape(name) + r"\][^\n]*\n(.*?)(?=^## \[|^<!-- historical-releases-footer -->|\Z)",
        content,
        re.MULTILINE | re.DOTALL,
    )
    if not match:
        raise ValueError(f"Missing changelog section [{name}]")
    return match.group(1).strip()


def release_notes(build_content, current_content, previous):
    if build_content != current_content:
        raise ValueError("CHANGELOG.md differs from the selected build; build the corrected changelog before promotion")
    tag = previous["tag_name"]
    link = re.search(r"^\[Unreleased\]: \S+/compare/([^\s]+)\.\.\.HEAD$", build_content, re.MULTILINE)
    if not link or link.group(1) != tag:
        raise ValueError("Unreleased comparison must start at the latest published release")
    if section(build_content, tag) != (previous.get("body") or "").strip():
        raise ValueError(
            f"[{tag}] differs from its published release notes. A rebase may have placed new notes "
            "under the previous release heading; move those notes back to [Unreleased] and rebuild."
        )
    notes = section(build_content, "Unreleased")
    if not re.search(r"^- ", notes, re.MULTILINE):
        raise ValueError("[Unreleased] has no release notes; correct the changelog and rebuild before promotion")
    return notes + "\n"


if __name__ == "__main__":
    build, current, previous, output = map(Path, sys.argv[1:])
    try:
        notes = release_notes(build.read_text(), current.read_text(), json.loads(previous.read_text()))
    except ValueError as error:
        sys.exit(str(error))
    output.write_text(notes)
