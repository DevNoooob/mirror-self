#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT
SRC=src/main/java/com/mirrorself
javac -d "$WORK_DIR" \
  "$SRC/common/Hashing.java" \
  "$SRC/memory/MemoryType.java" \
  "$SRC/memory/MemoryStatus.java" \
  "$SRC/memory/AssertionOrigin.java" \
  "$SRC/memory/Sensitivity.java" \
  "$SRC/memory/ReviewDecision.java" \
  "$SRC/memory/MemoryPolicy.java" \
  "$SRC/memory/SearchTerms.java" \
  "$SRC/memory/SensitiveDetector.java"
cat > "$WORK_DIR/DomainSmoke.java" <<'JAVA'
import com.mirrorself.memory.*;
import com.mirrorself.common.*;
public class DomainSmoke {
  public static void main(String[] args) {
    if (MemoryPolicy.initialStatus(AssertionOrigin.MODEL_INFERRED, Sensitivity.NORMAL)
        != MemoryStatus.PENDING_REVIEW) throw new AssertionError("inferred claim activated");
    if (MemoryPolicy.initialStatus(AssertionOrigin.USER_EXPLICIT, Sensitivity.NORMAL)
        != MemoryStatus.ACTIVE) throw new AssertionError("explicit claim not activated");
    if (MemoryPolicy.initialStatus(AssertionOrigin.USER_EXPLICIT, Sensitivity.SECRET)
        != MemoryStatus.PENDING_REVIEW) throw new AssertionError("secret claim activated");
    if (!SearchTerms.from("Java并发编程").contains("并发")) throw new AssertionError("missing Chinese tokens");
    if (!Hashing.sha256("abc").equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))
        throw new AssertionError("SHA-256 mismatch");
    System.out.println("DOMAIN SMOKE: PASSED");
  }
}
JAVA
javac -cp "$WORK_DIR" -d "$WORK_DIR" "$WORK_DIR/DomainSmoke.java"
java -cp "$WORK_DIR" DomainSmoke
