#!/usr/bin/env python3
"""REST API wrapper for RedPill Cloud bot."""

import json
import os
import sys
from datetime import datetime
from typing import Optional

from fastapi import FastAPI, HTTPException, Header, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

sys.path.insert(0, "/opt/redpill-bot")

from db import get_db, db_cursor, row_to_dict
from admin_handlers import admin_grant, admin_extend, admin_revoke, admin_reissue

app = FastAPI(title="RedPill Cloud API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

ADMIN_TOKEN = os.getenv("REDPILL_API_TOKEN", "changeme")
BOT_ADMIN_ID = int(os.getenv("ADMIN_ID", "0"))


def verify_admin(token: str = Header(...)):
    if token != ADMIN_TOKEN:
        raise HTTPException(403, "Forbidden")


def get_user_by_telegram(tg_id: int):
    with db_cursor() as (_, cur):
        cur.execute("SELECT * FROM users WHERE user_id = ?", (tg_id,))
        return row_to_dict(cur.fetchone())


# ---------- Public endpoints ----------

@app.get("/api/v1/ping")
def ping():
    return {"status": "ok", "time": datetime.utcnow().isoformat()}


@app.get("/api/v1/user/{tg_id}")
def get_user(tg_id: int):
    user = get_user_by_telegram(tg_id)
    if not user:
        raise HTTPException(404, "User not found")

    sub = None
    with db_cursor() as (_, cur):
        cur.execute(
            """
            SELECT * FROM subscriptions_v2
            WHERE user_id = ? AND status = 'active' AND expires_at > datetime('now')
            ORDER BY expires_at DESC LIMIT 1
            """,
            (tg_id,),
        )
        sub = row_to_dict(cur.fetchone())

    with db_cursor() as (_, cur):
        cur.execute(
            "SELECT COUNT(*) as device_count FROM subscription_devices WHERE user_id = ?",
            (tg_id,),
        )
        device_count = row_to_dict(cur.fetchone())["device_count"]

    with db_cursor() as (_, cur):
        cur.execute(
            "SELECT * FROM subscriptions WHERE user_id = ? AND active = 1 AND datetime(expires_at) > datetime('now') ORDER BY expires_at DESC LIMIT 1",
            (tg_id,),
        )
        legacy_sub = row_to_dict(cur.fetchone())

    return {
        "user_id": user["user_id"],
        "username": user.get("username"),
        "created_at": user.get("created_at"),
        "subscription": sub,
        "legacy_subscription": legacy_sub,
        "device_count": device_count,
    }


@app.get("/api/v1/user/{tg_id}/subscription")
def get_subscription(tg_id: int):
    with db_cursor() as (_, cur):
        cur.execute(
            """
            SELECT s.*, u.username
            FROM subscriptions_v2 s
            JOIN users u ON u.user_id = s.user_id
            WHERE s.user_id = ? AND s.status = 'active'
            ORDER BY s.expires_at DESC LIMIT 1
            """,
            (tg_id,),
        )
        sub = row_to_dict(cur.fetchone())

    if not sub:
        raise HTTPException(404, "No active subscription")

    with db_cursor() as (_, cur):
        cur.execute(
            "SELECT * FROM subscription_devices WHERE user_id = ?",
            (tg_id,),
        )
        devices = [row_to_dict(r) for r in cur.fetchall()]

    sub["devices"] = devices
    return sub


@app.get("/api/v1/user/{tg_id}/devices")
def get_devices(tg_id: int):
    with db_cursor() as (_, cur):
        cur.execute("SELECT * FROM subscription_devices WHERE user_id = ?", (tg_id,))
        devices = [row_to_dict(r) for r in cur.fetchall()]
    return {"devices": devices}


# ---------- Admin endpoints ----------

@app.post("/api/v1/admin/user/grant")
def admin_grant_endpoint(
    user_id: int,
    tariff: str = "pro_1m",
    days: int = 30,
    region: str = "nl",
    note: Optional[str] = None,
    token: str = Header(...),
):
    verify_admin(token=token)
    result = admin_grant(user_id, tariff, days, region, BOT_ADMIN_ID, note)
    return {"ok": True, "result": result}


@app.post("/api/v1/admin/user/extend")
def admin_extend_endpoint(
    user_id: int,
    days: int = 30,
    token: str = Header(...),
):
    verify_admin(token=token)
    result = admin_extend(user_id, days, BOT_ADMIN_ID)
    return {"ok": True, "result": str(result)}


@app.post("/api/v1/admin/user/revoke")
def admin_revoke_endpoint(
    user_id: int,
    token: str = Header(...),
):
    verify_admin(token=token)
    result = admin_revoke(user_id, BOT_ADMIN_ID)
    return {"ok": True, "result": str(result)}


@app.post("/api/v1/admin/user/reissue")
def admin_reissue_endpoint(
    user_id: int,
    device_index: int = 0,
    region: str = "nl",
    token: str = Header(...),
):
    verify_admin(token=token)
    result = admin_reissue(user_id, device_index, region)
    return {"ok": True, "result": result}


@app.get("/api/v1/admin/users")
def admin_list_users(token: str = Header(...)):
    verify_admin(token=token)
    with db_cursor() as (_, cur):
        cur.execute(
            """
            SELECT u.user_id, u.username, u.created_at,
                   s.tariff, s.expires_at, s.status,
                    (SELECT COUNT(*) FROM subscription_devices d WHERE d.user_id = u.user_id) as device_count
            FROM users u
            LEFT JOIN subscriptions_v2 s ON s.user_id = u.user_id AND s.status = 'active'
            ORDER BY u.user_id DESC
            LIMIT 100
            """
        )
        users = [row_to_dict(r) for r in cur.fetchall()]
    return {"users": users}


@app.get("/api/v1/admin/stats")
def admin_stats(token: str = Header(...)):
    verify_admin(token=token)
    with db_cursor() as (_, cur):
        cur.execute("SELECT COUNT(*) as total FROM users")
        total_users = row_to_dict(cur.fetchone())["total"]

        cur.execute(
            "SELECT COUNT(*) as active FROM subscriptions_v2 WHERE status = 'active' AND expires_at > datetime('now')"
        )
        active_subs = row_to_dict(cur.fetchone())["active"]

        cur.execute("SELECT COUNT(*) as total FROM subscription_devices")
        total_devices = row_to_dict(cur.fetchone())["total"]

    return {
        "total_users": total_users,
        "active_subscriptions": active_subs,
        "total_devices": total_devices,
    }


# ---------- Servers ----------

@app.get("/api/v1/servers")
def list_servers():
    with db_cursor() as (_, cur):
        cur.execute("SELECT * FROM vpn_servers WHERE is_enabled = 1")
        servers = [row_to_dict(r) for r in cur.fetchall()]
    return {"servers": servers}


@app.get("/api/v1/proxy")
def get_proxy(user_id: Optional[int] = Query(None)):
    socks_login = ""
    socks_password = ""
    if user_id:
        with db_cursor() as (_, cur):
            cur.execute(
                "SELECT login, password FROM socks_proxy_access WHERE user_id = ?",
                (user_id,)
            )
            row = cur.fetchone()
            if row:
                socks_login, socks_password = row
    if not socks_login or not socks_password:
        with db_cursor() as (_, cur):
            cur.execute("SELECT login, password FROM socks_proxy_access LIMIT 1")
            row = cur.fetchone()
            if row:
                socks_login, socks_password = row
    return {
        "host": "216.57.106.89",
        "port": 995,
        "protocol": "socks5",
        "socks_login": socks_login,
        "socks_password": socks_password
    }
