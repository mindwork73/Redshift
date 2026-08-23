#!/system/bin/sh
# Device-side smoke test of VPN_SERVICE_FD passing (run from /data/local/tmp):
#   adb push libsingbox.so /data/local/tmp/singbox-lx
#   adb push tfd.json tfd.sh /data/local/tmp && chmod 755 ...
#   adb shell timeout 6 /data/local/tmp/tfd.sh
# Expected log: "attaching tun inbound to VPN_SERVICE_FD=3" then
# "started at tunN". The final bind error is expected here because fd 3 is
# /dev/null and has no real interface address; with a real VpnService fd the
# address exists (the launcher adds it via Builder.addAddress).

exec 3</dev/null
cd /data/local/tmp || exit 1
VPN_SERVICE_FD=3 exec ./singbox-lx run -c /data/local/tmp/tfd.json -D /data/local/tmp/wd
