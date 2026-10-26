#!/usr/bin/env python3
"""Dependency-free structural guard, not a substitute for Maven/PostgreSQL integration tests."""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
ET.parse(ROOT / "pom.xml")
conversations = json.loads((ROOT / "fixtures/conversations.sample.json").read_text())
assert len(conversations) == 1
assert len(conversations[0]["mapping"]) == 3
schema = (ROOT / "src/main/resources/db/migration/V1__memory_foundation.sql").read_text()
expected = [
    "source_record", "memory_item", "memory_evidence", "memory_relation",
    "memory_embedding", "identity_claim", "identity_evidence",
    "share_profile", "share_grant", "ingestion_job", "audit_event"
]
for table in expected:
    assert re.search(r"CREATE TABLE " + table + r"\b", schema), table
assert "owner_id=?" in (ROOT / "src/main/java/com/mirrorself/memory/MemoryRepository.java").read_text()
assert 'model-egress-enabled: ${MIRROR_MODEL_EGRESS_ENABLED:false}' in (ROOT / 'src/main/resources/application.yml').read_text()
print(f"STRUCTURAL CHECK PASSED: {len(expected)} tables, valid pom and example JSON")
