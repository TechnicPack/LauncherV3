"""Explicitly refresh release-bundled TLS policy, or validate it offline with --check."""

import argparse
import base64
import datetime
import hashlib
import json
import os
from pathlib import Path
import re
import ssl
import tempfile
import urllib.parse
import urllib.request

ROOT_URL = "https://curl.se/ca/cacert.pem"
ONECRL_URL = "https://firefox.settings.services.mozilla.com/v1/buckets/security-state/collections/onecrl/records"
OUTPUT = Path(__file__).resolve().parents[1] / "src/main/resources/tls"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def decode(value):
    if not isinstance(value, str) or not value:
        raise ValueError("Missing Base64 value")
    return base64.b64decode(value, validate=True)


def der_items(data):
    offset = 0
    while offset < len(data):
        tag = data[offset]
        offset += 1
        if offset == len(data):
            raise ValueError("Truncated DER")
        size = data[offset]
        offset += 1
        if size & 128:
            count = size & 127
            if not count or offset + count > len(data) or data[offset] == 0:
                raise ValueError("Invalid DER length")
            size = int.from_bytes(data[offset:offset + count], "big")
            offset += count
            if size < 128:
                raise ValueError("Noncanonical DER length")
        if offset + size > len(data):
            raise ValueError("Truncated DER value")
        yield tag, data[offset:offset + size]
        offset += size


def validate_name(data):
    outer = list(der_items(data))
    if len(outer) != 1 or outer[0][0] != 48:
        raise ValueError("Invalid DER Name")
    for tag, rdn in der_items(outer[0][1]):
        attributes = list(der_items(rdn))
        if tag != 49 or not attributes:
            raise ValueError("Invalid DER RDN")
        for tag, attribute in attributes:
            pair = list(der_items(attribute))
            if tag != 48 or len(pair) != 2 or pair[0][0] != 6 or not pair[0][1]:
                raise ValueError("Invalid DER name attribute")


def validate_records(document):
    if not isinstance(document, dict) or not isinstance(document.get("data"), list):
        raise ValueError("OneCRL must contain a data array")
    seen = set()
    enabled = 0
    for record in document["data"]:
        if not isinstance(record, dict) or not isinstance(record.get("id"), str) or not record["id"]:
            raise ValueError("OneCRL record requires an ID")
        if record["id"] in seen:
            raise ValueError("Duplicate OneCRL ID: " + record["id"])
        seen.add(record["id"])
        if type(record.get("enabled")) is not bool:
            raise ValueError("OneCRL enabled must be Boolean")
        if not record["enabled"]:
            continue
        enabled += 1
        fields = {key for key in ("issuerName", "serialNumber", "subject", "pubKeyHash") if key in record}
        if fields == {"issuerName", "serialNumber"}:
            validate_name(decode(record["issuerName"]))
            if not decode(record["serialNumber"]):
                raise ValueError("Empty serial")
        elif fields == {"subject", "pubKeyHash"}:
            validate_name(decode(record["subject"]))
            if len(decode(record["pubKeyHash"])) != 32:
                raise ValueError("Invalid SPKI SHA-256")
        else:
            raise ValueError("Incomplete or ambiguous OneCRL matching form")
    if not seen or not enabled:
        raise ValueError("OneCRL requires enabled records")
    return len(seen), enabled


def validate_roots(pem):
    text = pem.decode("utf-8")
    blocks = re.findall(r"-----BEGIN CERTIFICATE-----\s*(.*?)\s*-----END CERTIFICATE-----", text, re.S)
    if (text.count("-----BEGIN CERTIFICATE-----") != len(blocks)
            or text.count("-----END CERTIFICATE-----") != len(blocks)):
        raise ValueError("Incomplete PEM certificate")
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    context.load_verify_locations(cadata="\n".join(
        "-----BEGIN CERTIFICATE-----\n" + block + "\n-----END CERTIFICATE-----" for block in blocks
    ))
    certificates = [base64.b64decode(re.sub(r"\s", "", block), validate=True) for block in blocks]
    if not certificates or len(set(certificates)) != len(certificates):
        raise ValueError("Root bundle must contain distinct certificates")
    if len(context.get_ca_certs(binary_form=True)) != len(certificates):
        raise ValueError("Root bundle contains unparsed or non-CA certificates")
    return len(certificates)


def serialize(value):
    return (json.dumps(value, indent=2, ensure_ascii=True, sort_keys=True) + "\n").encode("utf-8")


def validate_outputs(files):
    roots = validate_roots(files["cacert.pem"])
    records, enabled = validate_records(json.loads(files["onecrl.json"]))
    source = json.loads(files["sources.json"])
    timestamp = datetime.datetime.fromisoformat(source["retrievedAt"].replace("Z", "+00:00"))
    if timestamp.utcoffset() != datetime.timedelta(0):
        raise ValueError("retrievedAt must be UTC")
    expected_roots = {"url": ROOT_URL, "sha256": digest(files["cacert.pem"]), "certificateCount": roots}
    if source["roots"] != expected_roots:
        raise ValueError("Root provenance/checksum mismatch")
    crl = source["onecrl"]
    if (crl["url"] != ONECRL_URL or crl["sha256"] != digest(files["onecrl.json"])
            or crl["recordCount"] != records or crl["enabledCount"] != enabled
            or not isinstance(crl["etag"], str) or not crl["etag"]):
        raise ValueError("OneCRL provenance/checksum mismatch")
    return roots, records, enabled


class VerifiedRedirects(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        if urllib.parse.urlsplit(newurl).scheme != "https":
            raise ValueError("Refusing non-HTTPS redirect")
        return super().redirect_request(request, fp, code, msg, headers, newurl)


def download(url):
    opener = urllib.request.build_opener(VerifiedRedirects())
    with opener.open(url, timeout=30) as response:
        return response.read(), response.headers


def refresh(destination=OUTPUT, fetch=download):
    pem, _ = fetch(ROOT_URL)
    checksum, _ = fetch(ROOT_URL + ".sha256")
    published = checksum.decode("ascii").split()[0]
    if not re.fullmatch(r"[0-9a-fA-F]{64}", published) or digest(pem) != published.lower():
        raise ValueError("curl PEM SHA-256 verification failed")
    roots = validate_roots(pem)
    records = []
    visited = set()
    url = ONECRL_URL
    etag = None
    while url:
        if url in visited or urllib.parse.urlsplit(url)[:2] != urllib.parse.urlsplit(ONECRL_URL)[:2]:
            raise ValueError("OneCRL pagination loop or foreign origin")
        visited.add(url)
        payload, headers = fetch(url)
        page_etag = headers.get("ETag")
        if not page_etag or (etag is not None and page_etag != etag):
            raise ValueError("Missing or inconsistent OneCRL ETag")
        etag = page_etag
        page = json.loads(payload)
        if not isinstance(page, dict) or not isinstance(page.get("data"), list):
            raise ValueError("Invalid OneCRL page")
        records.extend(page["data"])
        next_page = headers.get("Next-Page")
        url = urllib.parse.urljoin(url, next_page) if next_page else None
    document = {"data": records}
    count, enabled = validate_records(document)
    records.sort(key=lambda record: record["id"])
    crl = serialize(document)
    files = {"cacert.pem": pem, "onecrl.json": crl}
    files["sources.json"] = serialize({
        "retrievedAt": datetime.datetime.now(datetime.timezone.utc).isoformat().replace("+00:00", "Z"),
        "roots": {"url": ROOT_URL, "sha256": digest(pem), "certificateCount": roots},
        "onecrl": {"url": ONECRL_URL, "etag": etag, "sha256": digest(crl),
                   "recordCount": count, "enabledCount": enabled},
    })
    validate_outputs(files)
    destination.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix=".tls-refresh-", dir=destination) as temporary:
        for name, data in files.items():
            path = Path(temporary) / name
            with path.open("wb") as output:
                output.write(data)
                output.flush()
                os.fsync(output.fileno())
        for name in files:
            os.replace(Path(temporary) / name, destination / name)
    return files


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Validate bundled inputs offline without modifying them")
    args = parser.parse_args()
    try:
        files = ({name: (OUTPUT / name).read_bytes() for name in ("cacert.pem", "onecrl.json", "sources.json")}
                 if args.check else refresh())
        roots, records, enabled = validate_outputs(files)
        print(f"TLS data verified: {roots} roots, {records} OneCRL records, {enabled} enabled")
        for name in ("cacert.pem", "onecrl.json"):
            print(f"{digest(files[name])}  {name}")
    except (OSError, ValueError, KeyError, TypeError, ssl.SSLError) as error:
        parser.exit(1, f"TLS data validation/refresh failed: {error}\n")


if __name__ == "__main__":
    main()
