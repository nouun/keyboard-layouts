#pragma once

#define SMOL_BOARD

/*
 * Layout
 */
#define SEMIMAK
#define DVORAK
#define QWERTY

/*
 * Optimizations
 */

// Disable Ooneshot
#define NO_ACTION_ONESHOT

// Disable music
#define NO_MUSIC_MODE

// Set max layers to 8
#define LAYER_STATE_8BIT


/*
 * Restro Shift
 */
#define RETRO_SHIFT 500

// Extend default timeout frot 100ms
#define AUTO_SHIFT_TIMEOUT 500

// Enable modifiers
#define AUTO_SHIFT_MODIFIERS

/*
 * Home row mods
 */
#define TAPPING_TERM 150
#define IGNORE_MOD_TAP_INTERRUPT
#define TAPPING_FORCE_HOLD

/*
 * Caps Word
 */
#define CAPS_WORD_IDLE_TIMEOUT 5000
#define DOUBLE_TAP_SHIFT_TURNS_ON_CAPS_WORD


/*
 * Combos
 */
#define COMBO_COUNT 2
