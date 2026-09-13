#!/usr/bin/env python3
"""Local Platform discovery for explicitly allowed packs on a real local Solder.

Run: python3 scripts/local-platform-fixture.py --pack local-runtime-test
No builds, downloads, or Minecraft runtime data are served by this fixture.
"""

import argparse
import html
import json
import re
import threading
from http.client import HTTPException
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, unquote, urlsplit, urlunsplit
from urllib.request import HTTPRedirectHandler, ProxyHandler, Request, build_opener


class NoRedirects(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # A local Solder must never redirect this fixture to a production service.
        return None


def solder_root(value):
    try:
        url = urlsplit(value)
        port = url.port
    except ValueError as error:
        raise argparse.ArgumentTypeError(str(error)) from error
    if (
        url.scheme not in ("http", "https")
        or url.hostname not in ("127.0.0.1", "localhost", "::1")
        or url.username is not None
        or url.password is not None
        or "?" in value
        or "#" in value
        or any(character.isspace() for character in value)
        or (port is not None and not 1 <= port <= 65535)
    ):
        raise argparse.ArgumentTypeError(
            "Solder URL must be an absolute http(s) loopback API root "
            "(127.0.0.1, localhost, or [::1]), without credentials, query, or fragment"
        )
    # Avoid DNS resolution for localhost; never use environment HTTP proxies.
    host = "[::1]" if url.hostname == "::1" else "127.0.0.1"
    authority = host if port is None else f"{host}:{port}"
    return urlunsplit((url.scheme, authority, url.path.rstrip("/") + "/", "", ""))


def pack_slug(value):
    if not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", value):
        raise argparse.ArgumentTypeError("pack slug must contain lowercase letters, digits, and hyphens")
    return value


class FixtureHandler(BaseHTTPRequestHandler):
    def respond(self, status, payload, content_type="application/json; charset=utf-8"):
        if content_type.startswith("application/json"):
            payload = json.dumps(payload, ensure_ascii=False)
        body = payload.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)

    def pack_metadata(self, slug):
        url = self.server.solder_url + "modpack/" + slug
        self.log_message("Solder GET %s", url)
        request = Request(url, headers={"Accept": "application/json"})
        try:
            # One opener per request avoids sharing mutable HTTP handler state across threads.
            opener = build_opener(ProxyHandler({}), NoRedirects())
            with opener.open(request, timeout=10) as response:
                metadata = json.load(response)
        except HTTPError as error:
            raise ValueError(f"Solder GET {url} returned HTTP {error.code}") from error
        except (URLError, OSError, HTTPException, UnicodeError, ValueError) as error:
            raise ValueError(f"Solder GET {url} failed: {error}") from error
        if not isinstance(metadata, dict) or metadata.get("error"):
            raise ValueError(f"Solder GET {url} did not return valid pack metadata")
        if (
            metadata.get("name") != slug
            or not isinstance(metadata.get("display_name"), str)
            or not metadata["display_name"].strip()
        ):
            raise ValueError(f"Solder GET {url} returned an invalid name or display_name")
        with self.server.stats_lock:
            stats = self.server.stats[slug].copy()
        return {
            "name": slug,
            "displayName": metadata["display_name"],
            "platformUrl": self.server.platform_url + "discover#" + slug,
            "solder": self.server.solder_url,
            "description": "Local discovery fixture. Builds and downloads come directly from Solder.",
            "ratings": 0,
            "runs": stats["run"],
            "installs": stats["install"],
            "isServer": False,
            "isOfficial": False,
            "feed": [],
        }

    def discover(self):
        packs = [self.pack_metadata(slug) for slug in self.server.packs]
        links = "\n".join(
            f'<li id="{pack["name"]}"><a href="platform://{pack["name"]}">'
            f'{html.escape(pack["displayName"])}</a> '
            f'({pack["name"]})</li>'
            for pack in packs
        )
        # Flying Saucer loads XMLResource, so serve well-formed XHTML without JS,
        # external assets, or a DTD that could cause an outbound network request.
        page = f'''<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>Local Solder discovery</title></head>
<body style="background-color: #202020; color: #eeeeee; padding: 24px;">
<h1>Local Solder discovery</h1>
<p>Only explicitly allowed packs are listed. Select a pack to open it in the launcher.</p>
<ul style="line-height: 2;">{links}</ul>
<p>Solder API: {html.escape(self.server.solder_url)}</p>
<p>Installation files and Minecraft runtime downloads are not served by this fixture.</p>
</body></html>'''
        self.respond(200, page, "text/html; charset=utf-8")

    def do_HEAD(self):
        # Launcher install/run pings use HEAD, not GET.
        self.do_GET()

    def do_GET(self):
        try:
            request = urlsplit(self.path)
            path = unquote(request.path).rstrip("/")
            if path == "/health":
                self.respond(200, {
                    "status": "ok",
                    "solder": self.server.solder_url,
                    "packs": self.server.packs,
                })
            elif path == "/news":
                self.respond(200, {"articles": []})
            elif path == "/discover":
                self.discover()
            elif path == "/search":
                query = parse_qs(request.query, keep_blank_values=True)
                if "q" not in query:
                    self.respond(400, {"error": "Search requires the q query parameter"})
                    return
                term = query["q"][0].strip().casefold()
                results = []
                for pack_id, slug in enumerate(self.server.packs, start=1):
                    pack = self.pack_metadata(slug)
                    if term in slug.casefold() or term in pack["displayName"].casefold():
                        results.append({
                            "id": pack_id,
                            "name": pack["displayName"],
                            "slug": slug,
                            "url": self.server.platform_url + "modpack/" + slug,
                        })
                self.respond(200, {"modpacks": results})
            else:
                match = re.fullmatch(r"/modpack/([^/]+)(?:/stat/(install|run))?", path)
                if match is None or match[1] not in self.server.stats:
                    self.respond(404, {"error": "Unknown route or pack not explicitly allowed"})
                    return
                slug, stat = match.groups()
                if stat is None:
                    self.respond(200, self.pack_metadata(slug))
                else:
                    with self.server.stats_lock:
                        self.server.stats[slug][stat] += 1
                        count = self.server.stats[slug][stat]
                    self.log_message("Local stat pack=%s event=%s count=%d", slug, stat, count)
                    self.respond(200, {"status": "ok", "pack": slug, "stat": stat, "count": count})
        except ValueError as error:
            self.log_error("%s", error)
            self.respond(502, {"error": str(error)})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=18082, help="127.0.0.1 listen port (default: 18082)")
    parser.add_argument(
        "--solder-url", type=solder_root, default="http://127.0.0.1:18081/api/",
        help="real loopback Solder API root (default: http://127.0.0.1:18081/api/)",
    )
    parser.add_argument("--pack", type=pack_slug, action="append", required=True,
                        help="allowed real Solder pack slug; repeat to allow multiple packs")
    args = parser.parse_args()
    if not 1 <= args.port <= 65535:
        parser.error("--port must be between 1 and 65535")
    with ThreadingHTTPServer(("127.0.0.1", args.port), FixtureHandler) as server:
        server.solder_url = args.solder_url
        server.platform_url = f"http://127.0.0.1:{args.port}/"
        server.packs = sorted(set(args.pack))
        server.stats = {slug: {"install": 0, "run": 0} for slug in server.packs}
        server.stats_lock = threading.Lock()
        print(f"Local Platform: {server.platform_url}", flush=True)
        print(f"Real Solder: {server.solder_url}", flush=True)
        print(f"Allowed packs: {', '.join(server.packs)}", flush=True)
        print("Stats are local to this process; /health reports fixture liveness only.", flush=True)
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            pass


if __name__ == "__main__":
    main()
