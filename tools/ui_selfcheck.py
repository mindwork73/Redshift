#!/usr/bin/env python3
"""Static self-check for the redesigned com.example.ui layer.

The sandbox has no JDK / Android SDK, so `./gradlew :app:assembleDebug` cannot run.
This script checks the classes of errors that would break that build or the runtime:
  1. brace/paren/bracket balance per file
  2. package declaration matches directory
  3. every com.example.ui[.*] import resolves to a real declaration
  4. every RedShiftState.<member> reference exists in Models.kt
  5. every t("key") / UiText.format("key") key exists in UiText or Trans
  6. unused imports (hygiene; these are warnings, not errors)
  7. required entry points exist with the frozen signatures
"""
import os
import re
import sys

ROOT = "app/src/main/java/com/example"
UI = os.path.join(ROOT, "ui")

NEW_FILES = []
for dirpath, _, files in os.walk(UI):
    for f in sorted(files):
        if f.endswith(".kt"):
            NEW_FILES.append(os.path.join(dirpath, f))

FROZEN = {os.path.join(UI, "Models.kt"), os.path.join(UI, "Localization.kt")}
EDITABLE = [f for f in NEW_FILES if f not in FROZEN]

problems = []


def read(p):
    # utf-8-sig: Models.kt carries a pre-existing UTF-8 BOM (left untouched on purpose).
    with open(p, encoding="utf-8-sig") as fh:
        return fh.read()


def strip_noise(src):
    """Single-pass scanner: strings/chars/templates first, comments second.

    Order matters — a Kotlin string may legitimately contain `//` (e.g. "vpn://..."),
    which a comment-first pass would eat together with the rest of the line.
    """
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if src.startswith('"""', i):
            j = src.find('"""', i + 3)
            i = n if j < 0 else j + 3
            out.append('""')
            continue
        if src.startswith("/*", i):
            j = src.find("*/", i + 2)
            i = n if j < 0 else j + 2
            out.append(" ")
            continue
        if src.startswith("//", i):
            j = src.find("\n", i)
            i = n if j < 0 else j
            continue
        if c == '"' or c == "'":
            quote, i = c, i + 1
            while i < n and src[i] != quote:
                if src[i] == "\\":
                    i += 1
                i += 1
            i += 1
            out.append('""')
            continue
        out.append(c)
        i += 1
    return "".join(out)


# ── 1. balance ────────────────────────────────────────────────────────────────
for path in NEW_FILES:
    body = strip_noise(read(path))
    for open_c, close_c in (("{", "}"), ("(", ")"), ("[", "]")):
        if body.count(open_c) != body.count(close_c):
            problems.append(
                f"BALANCE {path}: '{open_c}' {body.count(open_c)} vs '{close_c}' {body.count(close_c)}"
            )

# ── 2. package matches directory ──────────────────────────────────────────────
for path in NEW_FILES:
    m = re.search(r"^package\s+([\w.]+)", read(path), re.M)
    if not m:
        problems.append(f"PACKAGE {path}: no package declaration")
        continue
    pkg = m.group(1)
    expected = "com.example." + os.path.dirname(path).replace(ROOT + "/", "").replace("/", ".")
    if pkg != expected and not (pkg == "com.example.ui" and path.endswith("ui")):
        problems.append(f"PACKAGE {path}: declared '{pkg}', directory implies '{expected}'")

# ── collect declarations ──────────────────────────────────────────────────────
DECL = {}  # fully qualified name -> file
for path in NEW_FILES:
    src = read(path)
    pkg = re.search(r"^package\s+([\w.]+)", src, re.M).group(1)
    for m in re.finditer(
        r"^(?:@\w+(?:\([^)]*\))?\s+)*(?:internal |private |public |const |lateinit |)*(?:suspend )?"
        r"(?:fun|val|var|class|object|enum class|data class|interface)\s+"
        r"(?:<[^>]+>\s+)?([A-Za-z_]\w*)",
        src,
        re.M,
    ):
        DECL.setdefault(f"{pkg}.{m.group(1)}", path)
    # members of top-level objects are reachable as Object.member from the same package

OBJECT_MEMBERS = {}
for path in NEW_FILES:
    src = read(path)
    for obj in re.finditer(r"^object\s+(\w+)\s*\{(.*?)^\}", src, re.M | re.S):
        name, bodytext = obj.group(1), obj.group(2)
        members = set(re.findall(r"\b(?:val|var|fun)\s+([A-Za-z_]\w*)", bodytext))
        OBJECT_MEMBERS.setdefault(name, set()).update(members)

# RedShiftState members come from the frozen Models.kt
models = read(os.path.join(UI, "Models.kt"))
redshift_members = set(
    re.findall(r"\b(?:val|var|fun)\s+([A-Za-z_]\w*)", models.split("object RedShiftState")[1])
)
localization = read(os.path.join(UI, "Localization.kt"))
loc_members = set(
    re.findall(
        r"\b(?:val|var|fun)\s+([A-Za-z_]\w*)",
        localization.split("object LocalizationState")[1].split("object Trans")[0],
    )
)

# ── 3. internal imports resolve ───────────────────────────────────────────────
for path in EDITABLE:
    src = read(path)
    for m in re.finditer(r"^import\s+(com\.example\.[\w.]+)$", src, re.M):
        fq = m.group(1)
        if fq.startswith("com.example.service"):
            continue
        if fq == "com.example.BuildConfig":  # generated by AGP (buildConfig = true)
            continue
        if fq not in DECL:
            problems.append(f"IMPORT {path}: unresolved '{fq}'")

# ── 4. RedShiftState / LocalizationState member access ────────────────────────
for path in EDITABLE:
    src = read(path)
    for m in re.finditer(r"RedShiftState\.([A-Za-z_]\w*)", src):
        if m.group(1) not in redshift_members:
            problems.append(f"STATE {path}: RedShiftState.{m.group(1)} not declared in Models.kt")
    for m in re.finditer(r"LocalizationState\.([A-Za-z_]\w*)", src):
        if m.group(1) not in loc_members:
            problems.append(f"STATE {path}: LocalizationState.{m.group(1)} not declared")

# ── 5. i18n keys ──────────────────────────────────────────────────────────────
uitext = read(os.path.join(UI, "UiText.kt"))
new_keys = set(re.findall(r'^\s{8}"([a-z0-9_]+)" to mapOf', uitext, re.M))
trans_keys = set(re.findall(r'"([a-z0-9_]+)" to mapOf', localization))
all_keys = new_keys | trans_keys
used = set()
for path in EDITABLE:
    src = read(path)
    used |= set(re.findall(r'\bt\("([a-z0-9_]+)"\)', src))
    used |= set(re.findall(r'UiText\.format\("([a-z0-9_]+)"', src))
    used |= set(re.findall(r'UiText\.get\("([a-z0-9_]+)"', src))
    used |= set(re.findall(r'labelKey\s*=\s*"([a-z0-9_]+)"', src))
    used |= set(re.findall(r'^\s+[A-Z]\w+\("([a-z0-9_]+)"\)', src, re.M))
# keys referenced indirectly through data classes (onboarding slides, tabs)
for path in EDITABLE:
    src = read(path)
    used |= set(re.findall(r'"(onboarding_[a-z0-9_]+|nav_[a-z0-9_]+)"', src))

missing = sorted(k for k in used if k not in all_keys)
for k in missing:
    problems.append(f"I18N key '{k}' is used but not defined in UiText or Trans")

# ── 6. unused imports ────────────────────────────────────────────────────────
unused_total = 0
for path in EDITABLE:
    src = read(path)
    body = "\n".join(l for l in src.splitlines() if not l.startswith("import "))
    for m in re.finditer(r"^import\s+(?:[\w.]+\.)?([A-Za-z_]\w*)$", src, re.M):
        name = m.group(1)
        if name in {"getValue", "setValue", "provideDelegate"}:  # `by` delegates
            continue
        if not re.search(r"\b" + re.escape(name) + r"\b", body):
            problems.append(f"UNUSED-IMPORT {path}: {name}")
            unused_total += 1

# ── 7. entry points ──────────────────────────────────────────────────────────
shell = read(os.path.join(UI, "MainShell.kt"))
if not re.search(r"^fun MainAppContainer\(\)", shell, re.M):
    problems.append("ENTRY com.example.ui.MainAppContainer() missing")
theme = read(os.path.join(UI, "theme", "Theme.kt"))
if "fun MyApplicationTheme(" not in theme:
    problems.append("ENTRY com.example.ui.theme.MyApplicationTheme missing")

# ── report ────────────────────────────────────────────────────────────────────
print(f"files scanned : {len(NEW_FILES)} (frozen: {len(FROZEN & set(NEW_FILES))})")
print(f"i18n keys used: {len(used)}  | defined in UiText: {len(new_keys)} | in Trans: {len(trans_keys)}")
print(f"unused imports: {unused_total}")
if problems:
    print(f"\nPROBLEMS ({len(problems)}):")
    for p in problems:
        print("  -", p)
    sys.exit(1)
print("\nOK: no problems found")

# ── 8. unresolved references (a Kotlin symbol must be imported or declared locally) ──
KOTLIN_BUILTINS = {
    "if", "for", "while", "when", "catch", "return", "fun", "val", "var", "class", "object",
    "interface", "enum", "import", "package", "else", "do", "try", "throw", "in", "is", "as",
    "String", "Int", "Long", "Float", "Double", "Boolean", "Any", "Unit", "Nothing", "List",
    "MutableList", "Map", "Set", "Pair", "Triple", "ArrayOf", "listOf", "mapOf", "setOf",
    "mutableListOf", "mutableMapOf", "mutableSetOf", "emptyList", "emptyMap", "buildString",
    "compareBy", "compareByDescending", "print", "println", "require", "check", "error",
    "run", "let", "apply", "also", "with", "repeat", "lazy", "it", "this", "super", "null",
    "true", "false", "companion", "init", "constructor", "data", "sealed", "internal",
    "private", "public", "protected", "const", "suspend", "operator", "infix", "inline",
    "get", "set", "by", "where", "typeof", "append", "OptIn", "Suppress",
    "StringBuilder", "appendLine", "toIntOrNull", "toString", "copy", "let",
}

def declared_in(path):
    src = read(path)
    names = set()
    for m in re.finditer(
        r"\b(?:enum class|data class|sealed class|fun|val|var|class|object|interface|typealias)\s+"
        r"(?:<[^>]*>\s*)?(?:[\w<>]+\.)?([A-Za-z_]\w*)",
        src,
    ):
        names.add(m.group(1))
    # local variables: `val x = ...` / `var x by ...` inside bodies
    for m in re.finditer(r"\b(?:val|var)\s+([a-z_]\w*)\s*(?:=|:|by\b)", src):
        names.add(m.group(1))
    # lambda / function parameters
    for m in re.finditer(r"([a-z_]\w*)\s*:\s*[\w<>?,.\s()@]+\s*[,)=]", src):
        names.add(m.group(1))
    for m in re.finditer(r"^\s*([a-z_]\w*)\s*:\s*[\w<>?,.\s()@]+,\s*$", src, re.M):
        names.add(m.group(1))
    return names

PKG_NAMES = {}
for _p in NEW_FILES:
    _src = read(_p)
    _pkg = re.search(r"^package\s+([\w.]+)", _src, re.M).group(1)
    PKG_NAMES.setdefault(_pkg, set()).update(declared_in(_p))
    # enum entries: `enum class X(...) { A("k"), B("k") }`
    for _m in re.finditer(r"enum class \w+[^{]*\{([^}]*)\}", _src, re.S):
        for _e in re.finditer(r"([A-Z]\w*)\s*\(", _m.group(1)):
            PKG_NAMES[_pkg].add(_e.group(1))


def package_of(path):
    return re.search(r"^package\s+([\w.]+)", read(path), re.M).group(1)


def imported_in(path):
    src = read(path)
    names = set()
    for m in re.finditer(r"^import\s+([\w.]+?)(?:\s+as\s+(\w+))?$", src, re.M):
        names.add(m.group(2) or m.group(1).split(".")[-1])
    return names

unresolved = []
for path in EDITABLE:
    body = strip_noise(read(path))
    body = re.sub(r"^import[^\n]*\n", "", body, flags=re.M)
    known = (declared_in(path) | imported_in(path) | KOTLIN_BUILTINS
             | PKG_NAMES.get(package_of(path), set()))
    for m in re.finditer(r"(?<![\w.])([A-Za-z_]\w*)\s*(?=[(<])", body):
        name = m.group(1)
        if name in known:
            continue
        # generic type params like `fun <T> SegmentedControl` / `List<T>`
        if re.search(r"<\s*" + re.escape(name) + r"\s*>", body):
            continue
        unresolved.append(f"{os.path.basename(path)}: '{name}' is called/used but neither imported nor declared")

if unresolved:
    print(f"\nUNRESOLVED REFERENCES ({len(unresolved)}):")
    for u in sorted(set(unresolved)):
        print("  -", u)
else:
    print("unresolved references: 0")

# ── 9. every Icons.Filled.X used must be imported (member access is otherwise invisible) ──
icon_problems = []
for path in EDITABLE:
    src = read(path)
    imported_icons = set(
        re.findall(r"^import androidx\.compose\.material\.icons\.(?:automirrored\.)?filled\.(\w+)$",
                   src, re.M)
    )
    used_icons = set(re.findall(r"Icons\.(?:AutoMirrored\.)?Filled\.(\w+)", src))
    for name in sorted(used_icons - imported_icons):
        icon_problems.append(f"{os.path.basename(path)}: Icons.Filled.{name} used without an import")

if icon_problems:
    print(f"\nICON IMPORT PROBLEMS ({len(icon_problems)}):")
    for i in icon_problems:
        print("  -", i)
else:
    print("icon imports: all resolved")
