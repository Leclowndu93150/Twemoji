"use strict";

import { createHmac } from "node:crypto";

export function createMetrics({ endpoint, service, hmacKey, flushIntervalMs = 5000, maxBatch = 100 }) {
  const queue = [];
  let timer = null;

  function send(events) {
    const body = JSON.stringify({ service, events });
    const ts = String(Math.floor(Date.now() / 1000));
    const sig = createHmac("sha256", hmacKey).update(ts).update(".").update(body).digest("hex");
    fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Panel-Ts": ts,
        "X-Panel-Sig": sig,
      },
      body,
    }).catch(e => console.error("[metrics] send failed:", e.message));
  }

  function flush() {
    if (!queue.length) return;
    const batch = queue.splice(0, maxBatch);
    send(batch);
    if (queue.length) setImmediate(flush);
  }

  function track(kind, userKey, props) {
    queue.push({
      ts: Math.floor(Date.now() / 1000),
      kind,
      user_key: userKey ?? null,
      props: props ?? null,
    });
    if (queue.length >= maxBatch) flush();
  }

  function start() {
    if (timer) return;
    timer = setInterval(flush, flushIntervalMs);
    if (timer.unref) timer.unref();
  }

  function stop() {
    if (timer) { clearInterval(timer); timer = null; }
    flush();
  }

  return { track, start, stop, flush };
}
