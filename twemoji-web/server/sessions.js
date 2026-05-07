import { randomBytes } from "crypto";

const sessions = new Map();
const TTL_MS = 60 * 60 * 1000;

setInterval(() => {
    const now = Date.now();
    for (const [token, session] of sessions) {
        if (now - session.createdAt > TTL_MS) sessions.delete(token);
    }
}, 5 * 60 * 1000);

export function createSession(emojis) {
    const token = randomBytes(9).toString("base64url").slice(0, 12);
    sessions.set(token, {
        emojis,
        categories: [],
        createdAt: Date.now(),
    });
    return token;
}

export function getSession(token) {
    return sessions.get(token) ?? null;
}

export function updateSession(token, { categories, emojis }) {
    const session = sessions.get(token);
    if (!session) return false;
    if (categories) session.categories = categories;
    if (emojis) session.emojis = emojis;
    return true;
}
