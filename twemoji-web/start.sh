#!/bin/bash
set -e
cd "$(dirname "$0")"
echo "starting twemoji server..."
node server/index.js &
SERVER_PID=$!
echo "starting twemoji bot..."
python3 bot/bot.py
kill $SERVER_PID
