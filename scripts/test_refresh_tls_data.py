import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location("refresh_tls_data", Path(__file__).with_name("refresh-tls-data.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class RefreshTlsDataTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original = {name: (module.OUTPUT / name).read_bytes()
                        for name in ("cacert.pem", "onecrl.json", "sources.json")}

    def test_rejects_truncated_certificate_after_valid_roots(self):
        for suffix in (b"\n-----BEGIN CERTIFICATE-----\nAAAA", b"\n-----END CERTIFICATE-----"):
            with self.subTest(suffix=suffix), self.assertRaisesRegex(ValueError, "Incomplete PEM"):
                module.validate_roots(self.original["cacert.pem"] + suffix)

    def fetcher(self, mode):
        def fetch(url):
            if url == module.ROOT_URL:
                return self.original["cacert.pem"], {}
            if url == module.ROOT_URL + ".sha256":
                checksum = "0" * 64 if mode == "checksum" else module.digest(self.original["cacert.pem"])
                return checksum.encode("ascii"), {}
            if mode == "download":
                raise OSError("Injected download failure")
            records = json.loads(self.original["onecrl.json"])["data"]
            headers = {"ETag": '"snapshot"'}
            if mode == "schema":
                records[0]["enabled"] = "true"
            elif mode == "duplicate":
                records.append(records[0])
            elif mode == "foreign":
                headers["Next-Page"] = "https://example.invalid/records"
            elif mode == "loop":
                headers["Next-Page"] = module.ONECRL_URL
            elif mode in ("etag", "success"):
                split = len(records) // 2
                if url == module.ONECRL_URL:
                    headers["Next-Page"] = "?page=2"
                    records = records[:split]
                else:
                    records = records[split:]
                    if mode == "etag":
                        headers["ETag"] = '"changed"'
            return module.serialize({"data": records}), headers
        return fetch

    def test_failed_refresh_preserves_every_existing_byte(self):
        for mode in ("download", "checksum", "schema", "duplicate", "foreign", "loop", "etag"):
            with self.subTest(mode=mode), tempfile.TemporaryDirectory() as directory:
                destination = Path(directory)
                for name, data in self.original.items():
                    (destination / name).write_bytes(data)
                with self.assertRaises((OSError, ValueError)):
                    module.refresh(destination, self.fetcher(mode))
                self.assertEqual({p.name: p.read_bytes() for p in destination.iterdir()}, self.original)

    def test_pagination_preserves_all_records_and_exact_file_hashes(self):
        with tempfile.TemporaryDirectory() as directory:
            destination = Path(directory)
            module.refresh(destination, self.fetcher("success"))
            files = {p.name: p.read_bytes() for p in destination.iterdir()}
            self.assertEqual(module.validate_outputs(files), module.validate_outputs(self.original))
            expected = sorted(json.loads(self.original["onecrl.json"])["data"], key=lambda r: r["id"])
            self.assertEqual(json.loads(files["onecrl.json"])["data"], expected)
            self.assertEqual(files["cacert.pem"], self.original["cacert.pem"])


if __name__ == "__main__":
    unittest.main()
