import JSZip from "jszip";

function slugify(name) {
    return name
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "_")
        .replace(/^_+|_+$/g, "") || "twemoji_custom";
}

function sanitizeEmojiName(name) {
    return name
        .toLowerCase()
        .replace(/[^a-z0-9_\-]/g, "_")
        .replace(/^_+|_+$/g, "") || "emoji";
}

function buildPackMcmeta(packName, version) {
    const description = `${packName || "twemoji custom"} emoji pack`;

    if (version === "1.20.1") {
        return { pack: { pack_format: 15, description } };
    }
    if (version === "1.21.1") {
        return { pack: { pack_format: 48, description } };
    }
    if (version === "26.1") {
        return {
            pack: {
                description,
                min_format: [101, 1],
                max_format: [101, 1],
            },
        };
    }
    return { pack: { pack_format: 48, description } };
}

export async function buildZip(session, packName, version = "1.21.1") {
    const namespace = slugify(packName || "twemoji_custom");
    const zip = new JSZip();

    zip.file("pack.mcmeta", JSON.stringify(buildPackMcmeta(packName, version), null, 2));

    const emojiBase = `data/${namespace}/twemoji/emoji`;
    const categoryIconMap = {};
    const usedNames = new Set();

    for (const emoji of session.emojis) {
        let safeName = sanitizeEmojiName(emoji.name);
        let candidate = safeName;
        let i = 2;
        while (usedNames.has(candidate)) {
            candidate = `${safeName}${i++}`;
        }
        usedNames.add(candidate);

        const cat = emoji.category || null;
        const url = emoji.animated
            ? emoji.url
            : emoji.url.replace(/\.webp(\?|$)/, ".png$1");
        const descriptor = { name: candidate, url };
        if (cat) descriptor.category = cat;
        if (emoji.animated) descriptor.animated = true;
        if (emoji.aliases && emoji.aliases.length) descriptor.aliases = emoji.aliases;
        if (candidate !== emoji.name) {
            descriptor.aliases = [...(descriptor.aliases || []), emoji.name];
        }

        zip.file(`${emojiBase}/${candidate}.json`, JSON.stringify(descriptor, null, 2));

        if (cat && !categoryIconMap[cat]) {
            categoryIconMap[cat] = candidate;
        }
    }

    const categoriesInOrder = session.categories || [];
    const categoriesJson = categoriesInOrder.map((cat) => ({
        name: cat.name,
        icon: categoryIconMap[cat.name] || "",
    }));

    if (categoriesJson.length) {
        zip.file(
            `data/${namespace}/twemoji/categories.json`,
            JSON.stringify(categoriesJson, null, 2)
        );
    }

    return zip.generateAsync({ type: "nodebuffer", compression: "DEFLATE" });
}
