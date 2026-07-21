# Playback Mode Icon Design Spec

## Scope

This spec covers the newly designed local music playback mode icons:

- `mode-loop`: loop playback
- `mode-single`: single track repeat
- `mode-random`: shuffle playback

The existing list playback icon remains unchanged.

## Design Language

- Canvas: `64 x 64` SVG viewBox.
- Visual size: designed to sit inside a `24 x 24` UI slot.
- Stroke: rounded, `6px` at 64px source size.
- Corners and terminals: round caps and round joins.
- Arrow weight: matched to the existing list icon line weight and button scale.
- Color: uses `currentColor` so default, hover, and active states are controlled by CSS.

## Interaction States

- Default: icon inherits the button text color.
- Hover/focus: button background brightens; icon remains high contrast.
- Active: mode-specific button background indicates the current playback mode.

## Exported Assets

SVG source files are stored in:

- `web-player/assets/playback-modes/mode-loop.svg`
- `web-player/assets/playback-modes/mode-single.svg`
- `web-player/assets/playback-modes/mode-random.svg`

PNG exports are provided at:

- `16x16`
- `32x32`
- `48x48`
- `64x64`

The Android WebView asset copy is stored under:

- `app/src/main/assets/player/assets/playback-modes/`

## Usage

The local music mode button uses the SVG files for loop, single repeat, and shuffle modes. The list playback icon intentionally keeps its previous CSS line design to preserve the original visual identity.
