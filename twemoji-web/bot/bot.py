import os
import re
import subprocess
import requests
import discord
from dotenv import load_dotenv

load_dotenv()

BOT_TOKEN = os.environ["BOT_TOKEN"]
API_URL = os.getenv("API_URL", "http://localhost:3000")
SITE_URL = os.getenv("SITE_URL", "https://leclowndu93150.dev/twemoji")

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

    import random
    reply_line = random.choice(REPLIES)
    await message.reply(f"{reply_line}\n{SITE_URL}/editor?t={token}")


if __name__ == "__main__":
    server_proc = subprocess.Popen(
        ["node", "index.js"],
        cwd=os.path.join(os.path.dirname(__file__), "..", "server"),
    )
    try:
        client.run(BOT_TOKEN)
    finally:
        server_proc.terminate()
