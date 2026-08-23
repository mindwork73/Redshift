import zipfile, sys, os

# Repack the RedShift APK: replace the embedded sing-box binaries with our
# sing-box-lx build. Usage: python repack.py <base.apk> <newbin> <out.apk>
# Both zip entries receive the SAME file: the arm64 Go binary is used both as
# a JNI-less "shared library" (loaded path) and as the exec'd asset.

BASE = sys.argv[1] if len(sys.argv) > 1 else r"base.apk"
NEWBIN = sys.argv[2] if len(sys.argv) > 2 else r"libsingbox.so"
OUT = sys.argv[3] if len(sys.argv) > 3 else r"base-mod.apk"

REPLACE = {
    "lib/arm64-v8a/libsingbox.so",
    "assets/singbox/arm64-v8a/sing-box",
}

with open(NEWBIN, "rb") as f:
    newbin = f.read()

src = zipfile.ZipFile(BASE)
dst = zipfile.ZipFile(OUT, "w")

for info in src.infolist():
    if info.filename in REPLACE:
        ni = zipfile.ZipInfo(info.filename, date_time=info.date_time)
        ni.compress_type = info.compress_type
        ni.external_attr = info.external_attr
        dst.writestr(ni, newbin)
        print("replaced:", info.filename, "->", len(newbin), "bytes")
    else:
        dst.writestr(info, src.read(info.filename))

dst.close()
src.close()
print("OK ->", OUT)
