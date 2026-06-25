/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from "react";
import { apiRequest } from "../lib/api.js";
import { Plus, Trash2, Calendar, FileCheck, Package, ShoppingBag, Send } from "lucide-react";

interface TrackedOrder {
  id: string;
  customerName: string;
  phone: string;
  telegramUsername: string;
  comment: string;
  total: number;
  status: string;
  createdAt: string;
  items: Array<{
    productId: string;
    productTitleSnapshot: string;
    variantTitleSnapshot: string | null;
    imageSnapshot: string | null;
    quantity: number;
    unitPriceSnapshot: number;
    lineTotal: number;
    collectionTitleSnapshot: string;
  }>;
}

export default function MyOrdersView() {
  const [trackedOrderIds, setTrackedOrderIds] = useState<string[]>([]);
  const [ordersDetails, setOrdersDetails] = useState<TrackedOrder[]>([]);
  const [searchId, setSearchId] = useState("");
  const [accountPhone, setAccountPhone] = useState("");
  const [accountTelegramUsername, setAccountTelegramUsername] = useState("");
  const [accountLookupMode, setAccountLookupMode] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Load from localStorage on mount
  useEffect(() => {
    const savedPhone = localStorage.getItem("csc_account_phone") || "";
    const savedTelegram = localStorage.getItem("csc_account_telegram") || "";

    if (savedPhone && savedTelegram) {
      setAccountPhone(savedPhone);
      setAccountTelegramUsername(savedTelegram);
      setAccountLookupMode(true);
      void loadAccountOrders(savedPhone, savedTelegram);
    } else {
      void loadTrackedOrders();
    }
  }, []);

  const loadTrackedOrders = async () => {
    try {
      const stored = localStorage.getItem("csc_tracked_order_ids");
      const list = stored ? (JSON.parse(stored) as string[]) : [];
      setTrackedOrderIds(list);
      
      if (list.length > 0) {
        setIsLoading(true);
        const res = await fetch(`/api/orders/track?ids=${list.join(",")}`);
        if (res.ok) {
          const details = await res.json();
          setOrdersDetails(details);
          // Sync statuses inside storage
          const statusMap: Record<string, string> = {};
          details.forEach((o: any) => {
            statusMap[o.id] = o.status;
          });
          localStorage.setItem("csc_tracked_order_statuses", JSON.stringify(statusMap));
        } else {
          setErrorMsg("╨Э╨╡ ╤Г╨┤╨░╨╗╨╛╤Б╤М ╨╖╨░╨│╤А╤Г╨╖╨╕╤В╤М ╨┐╨╛╨┤╤А╨╛╨▒╨╜╨╛╤Б╤В╨╕ ╨┐╨╛ ╨╖╨░╨║╨░╨╖╨░╨╝.");
        }
      } else {
        setOrdersDetails([]);
      }
    } catch (e) {
      console.error(e);
      setErrorMsg("╨Ю╤И╨╕╨▒╨║╨░ ╨┐╤А╨╕ ╤З╤В╨╡╨╜╨╕╨╕ ╨╗╨╛╨║╨░╨╗╤М╨╜╤Л╤Е ╨┤╨░╨╜╨╜╤Л╤Е ╨▓╨╡╨┤╨╡╨╜╨╕╤П.");
    } finally {
      setIsLoading(false);
    }
  };

  const loadAccountOrders = async (phone: string, telegramUsername: string) => {
    try {
      setIsLoading(true);
      setErrorMsg(null);

      const details = await apiRequest<TrackedOrder[]>(
        "POST",
        "/api/account/orders/lookup",
        {
          phone,
          telegramUsername,
        }
      );

      setOrdersDetails(details);
      setAccountLookupMode(true);
      localStorage.setItem("csc_account_phone", phone);
      localStorage.setItem("csc_account_telegram", telegramUsername);

      const statusMap: Record<string, string> = {};
      details.forEach((o) => {
        statusMap[o.id] = o.status;
      });
      localStorage.setItem("csc_tracked_order_statuses", JSON.stringify(statusMap));

      if (details.length === 0) {
        setErrorMsg("╨Ч╨░╨║╨░╨╖╤Л ╨┐╨╛ ╤Г╨║╨░╨╖╨░╨╜╨╜╤Л╨╝ ╨┤╨░╨╜╨╜╤Л╨╝ ╨╜╨╡ ╨╜╨░╨╣╨┤╨╡╨╜╤Л.");
      }
    } catch (error: any) {
      console.error(error);
      setErrorMsg(error?.message || "╨Э╨╡ ╤Г╨┤╨░╨╗╨╛╤Б╤М ╨╖╨░╨│╤А╤Г╨╖╨╕╤В╤М ╨╖╨░╨║╨░╨╖╤Л ╨░╨║╨║╨░╤Г╨╜╤В╨░.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleLookupAccountOrders = async (e: React.FormEvent) => {
    e.preventDefault();
    const phone = accountPhone.trim();
    const telegramUsername = accountTelegramUsername.trim();

    if (!phone || !telegramUsername) {
      setErrorMsg("╨Т╨▓╨╡╨┤╨╕╤В╨╡ ╤В╨╡╨╗╨╡╤Д╨╛╨╜ ╨╕ Telegram username.");
      return;
    }

    await loadAccountOrders(phone, telegramUsername);
  };

  const handleAddOrderById = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    const targetId = searchId.trim();
    if (!targetId) return;

    if (trackedOrderIds.includes(targetId)) {
      setErrorMsg("╨н╤В╨╛╤В ╨╖╨░╨║╨░╨╖ ╤Г╨╢╨╡ ╨┤╨╛╨▒╨░╨▓╨╗╨╡╨╜ ╨▓ ╨┐╨░╨╜╨╡╨╗╤М ╨╛╤В╤Б╨╗╨╡╨╢╨╕╨▓╨░╨╜╨╕╤П.");
      setSearchId("");
      return;
    }

    try {
      setIsLoading(true);
      const res = await fetch(`/api/orders/track?ids=${targetId}`);
      if (res.ok) {
        const details = await res.json() as TrackedOrder[];
        if (details.length === 0) {
          setErrorMsg("╨Ч╨░╨║╨░╨╖ ╤Б ╤В╨░╨║╨╕╨╝ ID ╨╜╨╡ ╨╜╨░╨╣╨┤╨╡╨╜ ╨╜╨░ ╤Б╨╡╤А╨▓╨╡╤А╨╡. ╨Я╤А╨╛╨▓╨╡╤А╤М╤В╨╡ ╨┐╤А╨░╨▓╨╕╨╗╤М╨╜╨╛╤Б╤В╤М ╨▓╨▓╨╛╨┤╨░.");
        } else {
          const newList = [...trackedOrderIds, targetId];
          localStorage.setItem("csc_tracked_order_ids", JSON.stringify(newList));
          setTrackedOrderIds(newList);
          
          setOrdersDetails(prev => {
            // Avoid duplicates
            const filtered = prev.filter(o => o.id !== targetId);
            return [...filtered, details[0]];
          });

          // Sync status
          const initialStates = localStorage.getItem("csc_tracked_order_statuses");
          const statusMap = initialStates ? JSON.parse(initialStates) : {};
          statusMap[targetId] = details[0].status;
          localStorage.setItem("csc_tracked_order_statuses", JSON.stringify(statusMap));

          setSearchId("");
        }
      } else {
        setErrorMsg("╨Ю╤И╨╕╨▒╨║╨░ ╤Б╨╡╤А╨▓╨╡╤А╨░ ╨┐╤А╨╕ ╨┐╨╛╨╕╤Б╨║╨╡ ╨╖╨░╨║╨░╨╖╨░.");
      }
    } catch (err) {
      setErrorMsg("╨Э╨╡ ╤Г╨┤╨░╨╗╨╛╤Б╤М ╤Б╨╛╨╡╨┤╨╕╨╜╨╕╤В╤М╤Б╤П ╤Б ╤Б╨╡╤А╨▓╨╡╤А╨╛╨╝.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetAccountLookup = () => {
    localStorage.removeItem("csc_account_phone");
    localStorage.removeItem("csc_account_telegram");
    setAccountPhone("");
    setAccountTelegramUsername("");
    setAccountLookupMode(false);
    setErrorMsg(null);
    void loadTrackedOrders();
  };

  const handleRemoveTrackedOrder = (id: string) => {
    const nextList = trackedOrderIds.filter(item => item !== id);
    localStorage.setItem("csc_tracked_order_ids", JSON.stringify(nextList));
    setTrackedOrderIds(nextList);
    setOrdersDetails(prev => prev.filter(o => o.id !== id));
  };

  const getStatusLabelRu = (status: string) => {
    switch (status) {
      case "new":
        return { text: "╨Э╨╛╨▓╤Л╨╣ ╨╖╨░╨║╨░╨╖", color: "text-amber-400 border border-amber-500/30 bg-amber-950/10" };
      case "confirmed":
        return { text: "╨Ю╨┐╨╗╨░╤В╨░ ╨┐╨╛╨┤╤В╨▓╨╡╤А╨╢╨┤╨╡╨╜╨░", color: "text-blue-400 border border-blue-500/30 bg-blue-950/10" };
      case "processing":
        return { text: "╨Т ╤Б╨▒╨╛╤А╨╡ ╨╕ ╨╛╨▒╤А╨░╨▒╨╛╤В╨║╨╡", color: "text-violet-400 border border-violet-500/30 bg-violet-950/10" };
      case "completed":
        return { text: "╨Ю╤В╨│╤А╤Г╨╢╨╡╨╜ / ╨Т╤Л╨┐╨╛╨╗╨╜╨╡╨╜", color: "text-emerald-400 border border-emerald-500/30 bg-emerald-950/10" };
      case "cancelled":
        return { text: "╨Ч╨░╨║╨░╨╖ ╨╛╤В╨╝╨╡╨╜╨╡╨╜", color: "text-red-500 border border-red-500/30 bg-red-950/10" };
      default:
        return { text: status.toUpperCase(), color: "text-zinc-400 border border-zinc-500/30 bg-[#1F1F23]" };
    }
  };

  const getStatusStepIndex = (status: string) => {
    switch (status) {
      case "new": return 0;
      case "confirmed": return 1;
      case "processing": return 2;
      case "completed": return 3;
      case "cancelled": return -1;
      default: return 0;
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-30 space-y-10" id="my-orders-view">
      
      {/* Title block */}
      <div className="border border-[#1F1F23] bg-[#0D0D0F] p-7 sm:p-8 text-left relative overflow-hidden select-none">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,79,0,0.015),transparent)]"></div>
        <h1 className="font-display text-2xl font-black text-white uppercase tracking-tighter">
          ╨Ь╨╛╨╕ ╨Ч╨░╨║╨░╨╖╤Л ╨╕ ╨Ю╤В╤Б╨╗╨╡╨╢╨╕╨▓╨░╨╜╨╕╨╡
        </h1>
        <p className="text-[#52525B] font-mono text-base uppercase font-bold tracking-widest mt-1">
          ╨Ъ╨╛╨╜╤В╤А╨╛╨╗╤М ╨║╨╛╨╗╨╗╨╡╨║╤В╨╕╨▓╨╜╤Л╤Е ╤Б╨▒╨╛╤А╨╛╨▓ ╨╕ ╤Б╤В╨░╤В╤Г╤Б ╨┤╨╛╤Б╤В╨░╨▓╨║╨╕ ╨▓ ╤А╨╡╨░╨╗╤М╨╜╨╛╨╝ ╨▓╤А╨╡╨╝╨╡╨╜╨╕
        </p>
      </div>

        {/* Account lookup */}
        <div className="bg-[#0D0D0F] border border-[#1F1F23] p-6 space-y-5">
          <div>
            <h2 className="text-white font-mono text-base uppercase tracking-widest font-black">
              ╨Э╨░╨╣╤В╨╕ ╨▓╤Б╨╡ ╨╝╨╛╨╕ ╨╖╨░╨║╨░╨╖╤Л
            </h2>
            <p className="text-[#52525B] font-mono text-base uppercase font-bold tracking-wider mt-1">
              ╨Т╨▓╨╡╨┤╨╕╤В╨╡ ╤В╨╡╨╗╨╡╤Д╨╛╨╜ ╨╕ Telegram username, ╤Г╨║╨░╨╖╨░╨╜╨╜╤Л╨╡ ╨┐╤А╨╕ ╨╛╤Д╨╛╤А╨╝╨╗╨╡╨╜╨╕╨╕ ╨╖╨░╨║╨░╨╖╨░
            </p>
          </div>

          <form onSubmit={handleLookupAccountOrders} className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <input
              type="text"
              required
              placeholder="╨в╨╡╨╗╨╡╤Д╨╛╨╜"
              value={accountPhone}
              onChange={(e) => setAccountPhone(e.target.value)}
              className="w-full bg-[#0A0A0B] outline-none border border-[#1F1F23] focus:border-[#FF4F00] px-4 py-3 text-white rounded-none font-mono text-base uppercase"
              id="account-phone-input"
            />

            <input
              type="text"
              required
              placeholder="Telegram username"
              value={accountTelegramUsername}
              onChange={(e) => setAccountTelegramUsername(e.target.value)}
              className="w-full bg-[#0A0A0B] outline-none border border-[#1F1F23] focus:border-[#FF4F00] px-4 py-3 text-white rounded-none font-mono text-base"
              id="account-telegram-input"
            />

            <button
              type="submit"
              disabled={isLoading}
              className="bg-[#FF4F00] hover:bg-[#e04600] disabled:opacity-50 text-black px-6 py-3 font-mono font-black text-base uppercase tracking-widest cursor-pointer transition-colors"
            >
              ╨Э╨░╨╣╤В╨╕ ╨╝╨╛╨╕ ╨╖╨░╨║╨░╨╖╤Л
            </button>
          </form>

          {accountLookupMode && (
            <div className="flex flex-wrap items-center justify-between gap-4 border border-[#1F1F23] bg-black/20 px-4 py-3">
              <div className="font-mono text-base uppercase text-[#A1A1AA]">
                ╨Р╨║╨║╨░╤Г╨╜╤В-╨┐╨╛╨╕╤Б╨║ ╨░╨║╤В╨╕╨▓╨╡╨╜:
                <span className="text-white font-black ml-2">{accountPhone}</span>
                <span className="text-[#FF4F00] font-black ml-2">{accountTelegramUsername}</span>
              </div>
              <button
                type="button"
                onClick={handleResetAccountLookup}
                className="border border-[#1F1F23] bg-[#0A0A0B] px-3 py-3 text-zinc-300 hover:text-white hover:border-[#FF4F00] font-mono text-base uppercase font-bold tracking-widest transition-colors"
              >
                ╨Т╤Л╨╣╤В╨╕ ╨╕╨╖ ╨║╨░╨▒╨╕╨╜╨╡╤В╨░
              </button>
            </div>
          )}

          <div className="border-t border-[#1F1F23] pt-4 space-y-5">
            <div>
              <h3 className="text-zinc-300 font-mono text-base uppercase tracking-widest font-black">
                ╨а╤Г╤З╨╜╨╛╨╡ ╨╛╤В╤Б╨╗╨╡╨╢╨╕╨▓╨░╨╜╨╕╨╡ ╨┐╨╛ ID
              </h3>
              <p className="text-[#52525B] font-mono text-base uppercase font-bold tracking-wider mt-1">
                ╨Ь╨╛╨╢╨╜╨╛ ╨┤╨╛╨▒╨░╨▓╨╕╤В╤М ╨║╨╛╨╜╨║╤А╨╡╤В╨╜╤Л╨╣ ╨╖╨░╨║╨░╨╖ ╨┐╨╛ ╨╡╨│╨╛ ╨╕╨┤╨╡╨╜╤В╨╕╤Д╨╕╨║╨░╤В╨╛╤А╤Г
              </p>
            </div>

            <form onSubmit={handleAddOrderById} className="flex flex-col sm:flex-row gap-4">
              <div className="flex-1">
                <input
                  type="text"
                  required
                  placeholder="╨Т╨▓╨╡╨┤╨╕╤В╨╡ ID ╨╖╨░╨║╨░╨╖╨░ ╨┤╨╗╤П ╨╛╤В╤Б╨╗╨╡╨╢╨╕╨▓╨░╨╜╨╕╤П (╨╜╨░╨┐╤А╨╕╨╝╨╡╤А: ord-7f32...)"
                  value={searchId}
                  onChange={(e) => setSearchId(e.target.value)}
                  className="w-full bg-[#0A0A0B] outline-none border border-[#1F1F23] focus:border-[#FF4F00] px-4 py-3 text-white rounded-none font-mono text-base uppercase"
                  id="search-order-id-input"
                />
              </div>
              <button
                type="submit"
                disabled={isLoading}
                className="bg-[#16161A] hover:bg-[#1d1d22] disabled:opacity-50 text-white px-6 py-3 font-mono font-black text-base uppercase tracking-widest cursor-pointer transition-colors border border-[#1F1F23]"
              >
                ╨Ф╨╛╨▒╨░╨▓╨╕╤В╤М ╨┐╨╛ ID
              </button>
            </form>
          </div>

          {errorMsg && (
            <p className="text-red-500 font-mono text-base uppercase mt-1 font-bold">
              тЪа {errorMsg}
            </p>
          )}
        </div>

      {/* Orders pipeline listing */}
      <div className="space-y-6">
        {isLoading && ordersDetails.length === 0 && (
          <div className="text-center py-30">
            <div className="inline-block w-6 h-6 border-2 border-[#1F1F23] border-t-[#FF4F00] rounded-none animate-spin"></div>
            <p className="text-[#52525B] font-mono text-base uppercase tracking-widest mt-2">
              ╨б╨┐╨╡╤Ж╨╕╤Д╨╕╤Ж╨╕╤А╤Г╨╡╨╝ ╨┤╨░╨╜╨╜╤Л╨╡ ╨┐╨╛ ╨╖╨░╨║╨░╨╖╨░╨╝...
            </p>
          </div>
        )}

        {!isLoading && ordersDetails.length === 0 && (
          <div className="border border-dashed border-[#1F1F23] py-30 text-center text-[#52525B] font-mono text-base uppercase">
            ╨г ╨▓╨░╤Б ╨╡╤Й╨╡ ╨╜╨╡╤В ╤Б╨╛╤Е╤А╨░╨╜╨╡╨╜╨╜╤Л╤Е ╨╖╨░╨║╨░╨╖╨╛╨▓ ╨▓ ╤Н╤В╨╛╨╝ ╨▒╤А╨░╤Г╨╖╨╡╤А╨╡.<br />
            <span className="text-base text-zinc-500 mt-2 block">
              ╨б╨╛╨╖╨┤╨░╨╣╤В╨╡ ╨╜╨╛╨▓╤Л╨╣ ╨╖╨░╨║╨░╨╖ ╨┐╤А╨╕ ╨╛╤Д╨╛╤А╨╝╨╗╨╡╨╜╨╕╨╕ ╨║╨╛╤А╨╖╨╕╨╜╤Л ╨╕╨╗╨╕ ╨┤╨╛╨▒╨░╨▓╤М╤В╨╡ ID ╨▓╤Л╤И╨╡.
            </span>
          </div>
        )}

        {ordersDetails.map((order) => {
          const statusInfo = getStatusLabelRu(order.status);
          const stepIdx = getStatusStepIndex(order.status);
          const steps = ["╨а╨╡╨│╨╕╤Б╤В╤А╨░╤Ж╨╕╤П", "╨Ю╨┐╨╗╨░╤В╨░ ╨┐╨╛╨╗╤Г╤З╨╡╨╜╨░", "╨Т╤Л╨║╤Г╨┐ ╤В╨╛╨▓╨░╤А╨╛╨▓", "╨Ю╤В╨┐╤А╨░╨▓╨╗╨╡╨╜"];

          return (
            <div key={order.id} className="bg-[#0D0D0F] border border-[#1F1F23] p-6 space-y-6 relative" id={`tracked-card-${order.id}`}>
              
              {/* Order top bar info */}
              <div className="flex flex-wrap items-start justify-between gap-6 border-b border-[#1F1F23] pb-4">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="text-white font-mono text-base font-black uppercase tracking-wider">{order.id}</span>
                    <span className={`px-2 py-0.5 font-mono text-base uppercase font-black tracking-widest ${statusInfo.color}`}>
                      {statusInfo.text}
                    </span>
                  </div>
                  <div className="font-mono text-base text-[#52525B] font-bold">
                    ╨Ш╨╝╤П ╨┐╨╛╨╗╤Г╤З╨░╤В╨╡╨╗╤П: <span className="text-white uppercase font-black">{order.customerName}</span> | TG: <span className="text-[#FF4F00]">{order.telegramUsername}</span>
                  </div>
                  <div className="font-mono text-base text-[#52525B] block uppercase">
                    ╨Ф╨░╤В╨░ ╤Б╨╛╨╖╨┤╨░╨╜╨╕╤П: {new Date(order.createdAt).toLocaleString("ru-RU")}
                  </div>
                </div>

                <button
                  onClick={() => handleRemoveTrackedOrder(order.id)}
                  className="text-zinc-600 hover:text-red-500 transition-colors font-mono text-base uppercase font-bold tracking-widest inline-flex items-center gap-1 cursor-pointer border border-[#1F1F23] px-2 py-3 bg-[#0A0A0B]"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                  ╨г╨▒╤А╨░╤В╤М ╨╕╨╖ ╨┐╨░╨╜╨╡╨╗╨╕
                </button>
              </div>

              {/* Progress Steps Visual Block */}
              {stepIdx >= 0 && (
                <div className="bg-black/40 border border-[#1F1F23]/40 p-6 rounded-none space-y-5">
                  <div className="flex justify-between items-center text-center">
                    {steps.map((step, idx) => {
                      const isPast = idx <= stepIdx;
                      const isCurrent = idx === stepIdx;
                      return (
                        <div key={idx} className="flex-1 space-y-1 relative">
                          <div className={`mx-auto w-6 h-6 flex items-center justify-center font-mono text-base font-black tracking-tighter ${
                            isCurrent 
                              ? "bg-[#FF4F00] text-black ring-4 ring-[#FF4F00]/10" 
                              : isPast 
                              ? "bg-[#FF4F00] text-black" 
                              : "bg-[#16161A] text-[#52525B] border border-[#1F1F23]"
                          }`}>
                            {idx + 1}
                          </div>
                          <span className={`block font-mono text-base uppercase font-black tracking-widest text-center ${
                            isCurrent ? "text-[#FF4F00]" : isPast ? "text-white" : "text-[#52525B]"
                          }`}>
                            {step}
                          </span>
                        </div>
                      );
                    })}
                  </div>

                  {/* Operational helper info */}
                  {order.status === "new" && (
                    <div className="text-amber-500 font-mono text-[9.5px] uppercase font-bold text-center leading-relaxed">
                      тЪа ╨Ч╨░╨║╨░╨╖ ╨╖╨░╤А╨╡╨│╨╕╤Б╤В╤А╨╕╤А╨╛╨▓╨░╨╜! ╨Ф╨╗╤П ╨║╨╛╨╛╤А╨┤╨╕╨╜╨░╤Ж╨╕╨╕ ╨╛╨┐╨╗╨░╤В╤Л ╨╜╨░╨┐╨╕╤И╨╕╤В╨╡ ╨╝╨╡╨╜╨╡╨┤╨╢╨╡╤А╤Г ╨▓ Telegram: <strong className="text-white">@White_Blooming</strong>
                    </div>
                  )}
                  {order.status === "confirmed" && (
                    <div className="text-blue-400 font-mono text-[9.5px] uppercase font-bold text-center leading-relaxed">
                      тЬУ ╨б╤А╨╡╨┤╤Б╤В╨▓╨░ ╨┐╨╛╨╗╤Г╤З╨╡╨╜╤Л ╨╕ ╨╖╨░╤А╨╡╨╖╨╡╤А╨▓╨╕╤А╨╛╨▓╨░╨╜╤Л ╨┤╨╗╤П ╨▓╤Л╨║╤Г╨┐╨░ ╨▓ ╤В╨╡╨║╤Г╤Й╨╡╨╝ ╨╛╨▒╤Й╨╡╨╝ ╤Б╨▒╨╛╤А╨╡. ╨Ю╨╢╨╕╨┤╨░╨╣╤В╨╡ ╨▓╤Л╨║╤Г╨┐╨░!
                    </div>
                  )}
                  {order.status === "processing" && (
                    <div className="text-violet-400 font-mono text-[9.5px] uppercase font-bold text-center leading-relaxed">
                      тЬИ ╨в╨╛╨▓╨░╤А╤Л ╨╖╨░╨║╨░╨╖╨░╨╜╤Л ╤Г ╨┐╨╛╤Б╤В╨░╨▓╤Й╨╕╨║╨╛╨▓ ╨╕ ╨║╨╛╨╜╤Б╨╛╨╗╨╕╨┤╨╕╤А╤Г╤О╤В╤Б╤П ╨╜╨░ ╤Б╨║╨╗╨░╨┤╨╡ ╨┐╨╡╤А╨╡╤Б╤Л╨╗╨║╨╕.
                    </div>
                  )}
                  {order.status === "completed" && (
                    <div className="text-emerald-400 font-mono text-[9.5px] uppercase font-bold text-center leading-relaxed">
                      ЁЯУж ╨Ч╨░╨║╨░╨╖ ╤Г╤Б╨┐╨╡╤И╨╜╨╛ ╤Б╨╛╨▒╤А╨░╨╜ ╨╕ ╨╛╤В╨│╤А╤Г╨╢╨╡╨╜ ╨▓ ╨▓╨░╤И ╤А╨╡╨│╨╕╨╛╨╜! ╨б╨┐╨░╤Б╨╕╨▒╨╛ ╨╖╨░ ╤Г╤З╨░╤Б╤В╨╕╨╡ ╨▓ ╨│╤А╤Г╨┐╨┐╨╛╨▓╨╛╨╝ ╤Б╨▒╨╛╤А╨╡!
                    </div>
                  )}
                </div>
              )}

              {/* Order items snapshots list */}
              <div className="border border-[#1F1F23] bg-black/20 divide-y divide-[#1F1F23] px-4 py-3">
                {order.items?.map((item, idx) => (
                  <div key={idx} className="flex gap-6 py-3 first:pt-1 last:pb-1">
                    <img
                      src={item.imageSnapshot || "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?q=80&w=40"}
                      alt="Product"
                      className="w-10 h-10 object-cover bg-[#0D0D0F] border border-[#1F1F23]"
                      referrerPolicy="no-referrer"
                    />
                    <div className="flex-1 min-w-0 font-mono text-base flex flex-col justify-between">
                      <div>
                        <span className="text-white font-black block uppercase truncate">{item.productTitleSnapshot}</span>
                        <span className="text-[#52525B] block uppercase mt-0.5">╨б╨▒╨╛╤А ╨╖╨░╨║╨░╨╖╨░: {item.collectionTitleSnapshot}</span>
                        {item.variantTitleSnapshot && (
                          <span className="text-[#FF4F00] block uppercase mt-0.5 font-semibold">╨Ю╨┐╤Ж╨╕╤П: {item.variantTitleSnapshot}</span>
                        )}
                      </div>
                      <div className="flex justify-between items-baseline mt-1 text-[#52525B]">
                        <span>╨Ъ╨╛╨╗╨╕╤З╨╡╤Б╤В╨▓╨╛: {item.quantity} x @ {item.unitPriceSnapshot} тВ╜</span>
                        <span className="text-white font-black">{item.lineTotal} тВ╜</span>
                      </div>
                    </div>
                  </div>
                ))}

                {order.comment && (
                  <div className="pt-3 font-sans text-base text-[#A1A1AA] bg-black/40 p-2.5 mt-2 flex items-start gap-1.5 leading-relaxed">
                    <span className="text-[#52525B] font-mono font-bold uppercase text-base shrink-0 mt-0.5">╨Т╨░╤И ╨║╨╛╨╝╨╝╨╡╨╜╤В╨░╤А╨╕╨╣:</span>
                    <span>"{order.comment}"</span>
                  </div>
                )}
              </div>

              {/* Operational Total sum */}
              <div className="flex justify-between items-baseline font-mono text-base border-t border-[#1F1F23] pt-4 text-[#52525B]">
                <span>╨Ш╨в╨Ю╨У ╨б ╨Ю╨Я╨Ы╨Р╨з╨Х╨Э╨Э╨л╨Ь ╨б╨С╨Ю╨а╨Ю╨Ь:</span>
                <span className="text-[#FF4F00] text-base font-black font-mono">{order.total} тВ╜</span>
              </div>

            </div>
          );
        })}
      </div>
    </div>
  );
}
