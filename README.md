# Twemoji

Adds Twitter-style emoji to Minecraft chat, signs, books, and tooltips. Type `:name:` to insert one.

Built-in set: the full Twemoji sheet (smileys, hands, flags, etc.) ships with the mod and is always available. Custom emojis on top of that come from server datapacks.

## Adding custom emojis (datapack)

Custom emojis live in a server datapack at `data/<namespace>/twemoji/emoji/`. The file name (without `.png`) is the shortcode you type between colons.

The server scans datapacks on startup and on `/reload`, then pushes the emoji set to every connected client. Singleplayer and dedicated servers both work the same way (singleplayer uses the integrated server). Vanilla clients connecting to a modded server still work, they just don't get custom emojis.

### Static emojis

Drop a PNG into `data/<namespace>/twemoji/emoji/<name>.png`.

- Square images work best. They get scaled to roughly 8x8 in chat.
- Transparency is supported.
- Typing `:<name>:` inserts it.

Example: `data/mypack/twemoji/emoji/derp.png` lets you type `:derp:`.

### Animated emojis

Same folder, but provide two files:

- `data/<namespace>/twemoji/emoji/<name>.png` - frames stacked vertically. Each frame must be square. Width is the frame size, height is `width * frame_count`.
- `data/<namespace>/twemoji/emoji/<name>.png.mcmeta` - animation metadata, same format as vanilla animated textures.

Minimal mcmeta:

```json
{
  "animation": {
    "frametime": 2
  }
}
```

`frametime` is in ticks (20 ticks = 1 second). With no `frames` array, frames play in order on a loop.

Custom frame order and per-frame timing:

```json
{
  "animation": {
    "frametime": 2,
    "frames": [
      0,
      1,
      2,
      { "index": 1, "time": 4 },
      0
    ]
  }
}
```

### Datapack layout

```
mypack/
  pack.mcmeta
  data/
    mypack/
      twemoji/
        emoji/
          derp.png
          dance.png
          dance.png.mcmeta
```

Drop the datapack into a world's `datapacks/` folder (singleplayer) or the server's `world/datapacks/` (dedicated). Run `/reload` after adding new files. The mod must be present on both server and client for custom emojis to sync.

### Name collisions

If two datapacks define the same `<name>` (across namespaces), the second occurrence becomes `name1`, the third `name2`, and so on. A warning is logged naming the source files.

## Skin tones

For built-in Twemoji emojis where the shortcode matches a known person/hand emoji, the mod looks for tone variants and uses them automatically. Set your preferred tone with `/twemoji skin <0-5>` (0 = default).

## Live shortcode conversion

When you type `:name:` and close it with `:` (or hit space), the mod replaces it with the emoji glyph immediately, in chat, sign edits, and book edits.

## Notes

- Built-in Twemoji codepoints live in the supplementary private use area (`U+F0000+`).
- Custom static emojis use codepoints from `U+E000` upward.
- Custom animated emojis use codepoints from `U+F000` upward.
- These ranges are private use so they will not collide with normal text.

## License

This project is split between two licensing categories:

**Source code, build scripts, and configuration** are All Rights Reserved. You may view and fork on the hosting platform for the purpose of submitting a pull request, but you may not copy, modify, redistribute, or use this code in derivative works without prior written permission.

**Bundled image assets and shortcode/remapping data** are licensed under [CC-BY 4.0](https://creativecommons.org/licenses/by/4.0/), inherited from upstream:

- Twemoji graphics - Copyright (c) 2018 Twitter, Inc. and other contributors, CC-BY 4.0.
- Pixel art adaptation and resource pack layout - Copyright (c) 2020 Amber ([PixelTwemojiMC](https://github.com/AmberWat/PixelTwemojiMC)), CC-BY 4.0.

When redistributing this project (in source or binary form), the asset attribution above must be preserved. See `LICENSE` for full terms.
