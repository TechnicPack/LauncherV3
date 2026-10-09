import importlib.util
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("release_notes", Path(__file__).with_name("release-notes.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ReleaseNotesTest(unittest.TestCase):
    previous = {"tag_name": "v4.0-1", "body": "### Fixed\n\n- Previous fix."}
    content = (
        "## [Unreleased]\n\n### Fixed\n\n- New fix.\n\n"
        "## [v4.0-1] - 2026-09-25\n\n### Fixed\n\n- Previous fix.\n\n"
        "<!-- historical-releases-footer -->\n"
        "[Unreleased]: https://github.com/TechnicPack/LauncherV3/compare/v4.0-1...HEAD\n"
    )

    def test_emits_only_new_release_notes(self):
        self.assertEqual(module.release_notes(self.content, self.content, self.previous),
                         "### Fixed\n\n- New fix.\n")

    def test_rejects_notes_rebased_under_previous_release_even_with_unreleased_notes(self):
        misplaced = self.content.replace("- Previous fix.", "- Misplaced fix.\n- Previous fix.")
        with self.assertRaisesRegex(ValueError, "rebase may have placed"):
            module.release_notes(misplaced, misplaced, self.previous)

    def test_rejects_empty_unreleased_instead_of_substituting_commit_messages(self):
        empty = self.content.replace("- New fix.", "")
        with self.assertRaisesRegex(ValueError, "no release notes"):
            module.release_notes(empty, empty, self.previous)

    def test_rejects_changelog_from_different_build(self):
        with self.assertRaisesRegex(ValueError, "differs from the selected build"):
            module.release_notes(self.content, self.content.replace("New fix", "Later fix"), self.previous)

    def test_rejects_already_promoted_baseline(self):
        with self.assertRaisesRegex(ValueError, "latest published release"):
            module.release_notes(self.content, self.content, {"tag_name": "v4.0-2", "body": "- Other fix."})


if __name__ == "__main__":
    unittest.main()
