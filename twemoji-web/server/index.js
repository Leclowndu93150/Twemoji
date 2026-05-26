import express from "express";
import cors from "cors";
import crypto from "node:crypto";
import { fileURLToPath } from "url";
import { dirname, join } from "path";
import { createSession, getSession, updateSession } from "./sessions.js";
import { buildZip } from "./export.js";
import { createMetrics } from "./metrics.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const PORT = process.env.PORT || 3000;

const metrics = process.env.PANEL_ENDPOINT && process.env.PANEL_HMAC_KEY
    ? createMetrics({
        endpoint: process.env.PANEL_ENDPOINT,
        service: "twemoji",
        hmacKey: process.env.PANEL_HMAC_KEY,
    })
    : null;
if (metrics) metrics.start();

function hashIp(req) {
    const ip = req.headers["x-real-ip"] || req.headers["x-forwarded-for"]?.split(",")[0]?.trim() || req.ip || "";
    if (!ip) return null;
    return crypto.createHash("sha256").update(ip).digest("hex").slice(0, 16);
}

const app = express();
app.set("trust proxy", true);
app.use(cors());
app.use(express.json());
app.use(express.static(join(__dirname, "../site")));

app.post("/api/session", (req, res) => {
    const { emojis } = req.body;
    if (!Array.isArray(emojis) || emojis.length === 0) {
        return res.status(400).json({ error: "emojis array required" });
    }
    const token = createSession(emojis);
    metrics?.track("session_created", hashIp(req), { emoji_count: emojis.length });
    res.json({ token });
});

app.get("/api/session/:token", (req, res) => {
    const session = getSession(req.params.token);
    if (!session) return res.status(404).json({ error: "session not found or expired" });
    res.json(session);
});

app.put("/api/session/:token", (req, res) => {
    const { categories, emojis } = req.body;
    const ok = updateSession(req.params.token, { categories, emojis });
    if (!ok) return res.status(404).json({ error: "session not found or expired" });
    res.json({ ok: true });
});

app.get("/api/export/:token", async (req, res) => {
    const session = getSession(req.params.token);
    if (!session) return res.status(404).json({ error: "session not found or expired" });

    const packName = req.query.name || "twemoji_custom";
    const version = req.query.version || "1.21.1";
    try {
        const buf = await buildZip(session, packName, version);
        const filename = packName.replace(/[^a-z0-9_\-]/gi, "_") + ".zip";
        res.setHeader("Content-Type", "application/zip");
        res.setHeader("Content-Disposition", `attachment; filename="${filename}"`);
        metrics?.track("export", hashIp(req), { mc_version: version, name: packName });
        res.send(buf);
    } catch (e) {
        metrics?.track("export_failed", hashIp(req), { mc_version: version });
        res.status(500).json({ error: "export failed" });
    }
});

app.get("/editor", (req, res) => {
    res.sendFile(join(__dirname, "../site/editor.html"));
});

app.listen(PORT, () => console.log(`twemoji server running on :${PORT} :3`));
