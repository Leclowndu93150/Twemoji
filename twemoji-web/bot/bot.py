import os
import random
import re

import discord
import requests
from dotenv import load_dotenv

from metrics import Metrics

load_dotenv()

BOT_TOKEN = os.environ["BOT_TOKEN"]
API_URL = os.getenv("API_URL", "http://localhost:3000")
SITE_URL = os.getenv("SITE_URL", "https://leclowndu93150.dev/twemoji")
PANEL_ENDPOINT = os.getenv("PANEL_ENDPOINT")
PANEL_HMAC_KEY = os.getenv("PANEL_HMAC_KEY")

EMOJI_PATTERN = re.compile(r"<(a?):(\w+):(\d+)>")

REPLIES = [
    "here ya go!",
    "your emojis are ready to be sorted",
    "your datapack awaits",
    "have fun with your emojis",
]

intents = discord.Intents.default()
intents.message_content = True
client = discord.Client(intents=intents)

metrics = None
if PANEL_ENDPOINT and PANEL_HMAC_KEY:
    metrics = Metrics(endpoint=PANEL_ENDPOINT, service="bot", hmac_key=PANEL_HMAC_KEY)
    metrics.start()


def parse_emojis(content):
    emojis = []
    seen_ids = set()
    for match in EMOJI_PATTERN.finditer(content):
        animated_flag, name, emoji_id = match.group(1), match.group(2), match.group(3)
        if emoji_id in seen_ids:
            continue
        seen_ids.add(emoji_id)
        animated = animated_flag == "a"
        if animated:
            url = f"https://cdn.discordapp.com/emojis/{emoji_id}.gif?size=64"
        else:
            url = f"https://cdn.discordapp.com/emojis/{emoji_id}.png?size=64&quality=lossless"
        emojis.append({"name": name, "url": url, "animated": animated})
    return emojis


@client.event
async def on_ready():
    print(f"logged in as {client.user} :3")


@client.event
async def on_message(message):
    if message.author.bot:
        return
    if not message.content.startswith("!twemoji"):
        return

    body = message.content[len("!twemoji"):]
    emojis = parse_emojis(body)

    if metrics:
        metrics.track(
            "command_used",
            user_key=str(message.author.id),
            props={"cmd": "twemoji", "emoji_count": len(emojis)},
        )

    if not emojis:
        await message.reply("no emojis found!! put some custom emojis after !twemoji :3")
        return

    try:
        resp = requests.post(f"{API_URL}/api/session", json={"emojis": emojis}, timeout=5)
        resp.raise_for_status()
        token = resp.json()["token"]
    except Exception as e:
        await message.reply(f"something broke :( try again maybe? ({e})")
        return

    reply_line = random.choice(REPLIES)
    await message.reply(f"{reply_line}\n{SITE_URL}/editor?t={token}")


if __name__ == "__main__":
    client.run(BOT_TOKEN)
