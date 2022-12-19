#include QMK_KEYBOARD_H

#include "keycodes.h"

/*
 * Notes:
 *  - Retro Shift is enabled and set to 200ms by
 *      default so holding a key for 200ms will send
 *      the shifted variant.
 *  - Double tapping space will insert a period.
 *  - Any key row with twe lines indicates `LT` key,
 *      the top is the key sent when pressed and the
 *      bottom is the key when held.
 *  - Any key with `x` in set to `KC_NO` and is only
 *      used to indicate the key which is held to
 *      activate the layer.
 *  - Homerow mods: SCGA
 */

/*
 * Layers
 */
enum layer_names {
#ifdef SEMIMAK
	_SEMIMAK,
#endif
#ifdef DVORAK
	_DVORAK,
#endif
#ifdef QWERTY
	_QWERTY,
#endif
	MOD1,
	MOD2,
	OTHER,
};


/*
 * Layer switching
 */
enum custom_keycodes {
	LYR_SMK = SAFE_RANGE,
	LYR_DVK,
	LYR_QTY,
};


/*
 * Handle key presses
 */
bool process_record_user(uint16_t keycode, keyrecord_t *record) {
	switch(keycode) {
#ifdef SEMIMAK
	case LYR_SMK:
		if (record->event.pressed) {
			set_single_persistent_default_layer(_SEMIMAK);
		}
		return false;
#endif
#ifdef DVORAK
	case LYR_DVK:
		if (record->event.pressed) {
			set_single_persistent_default_layer(_DVORAK);
		}
		return false;
#endif
#ifdef QWERTY
	case LYR_QTY:
		if (record->event.pressed) {
			set_single_persistent_default_layer(_QWERTY);
		}
		return false;
#endif
	default:
		return true;
	}
}


/*
 * Enable Auto Shift for MT keys
 */
bool get_custom_auto_shifted_key(uint16_t keycode, keyrecord_t *cecord) {
	// These are required to be split into different switch statements
	// as some of them may be duplicated throughout layouts which have
	// a matching key position. For example, DVRK_A and QWRT_A are both
	// assigned to LSFT_T(KC_A).
#ifdef SEMIMAK
	switch(keycode) {
	case SMMK_S:
	case SMMK_R:
	case SMMK_N:
	case SMMK_T:
	case SMMK_D:
	case SMMK_E:
	case SMMK_A:
	case SMMK_I:
		return true;
	default:
		break;
	}
#endif
#ifdef DVORAK
	switch(keycode) {
	case DVRK_A:
	case DVRK_O:
	case DVRK_E:
	case DVRK_U:
	case DVRK_H:
	case DVRK_T:
	case DVRK_N:
	case DVRK_S:
		return true;
	default:
		break;
	}
#endif
#ifdef QWERTY
	switch(keycode) {
	case QWRT_A:
	case QWRT_S:
	case QWRT_D:
	case QWRT_F:
	case QWRT_H:
	case QWRT_J:
	case QWRT_K:
	case QWRT_L:
		return true;
	default:
		break;
	}
#endif
	return false;
}

/*
 * Combos
 */
enum combo_events {
  CMB_EMAIL,
  CMB_,
  COMBO_LENGTH
};

const uint16_t PROGMEM combo_email[] = {KC_X, KC_J, COMBO_END};
const uint16_t PROGMEM combo_[] = {KC_X, KC_N, COMBO_END};

combo_t key_combos[COMBO_COUNT] = {
	[CMB_EMAIL] = COMBO_ACTION(combo_email),
	[CMB_] = COMBO_ACTION(combo_),
};

void process_combo_event(uint16_t combo_index, bool pressed) {
	switch(combo_index) {
	case CMB_EMAIL:
		if (pressed) {
			SEND_STRING("john.doe@example.com");
		}
		break;
	case CMB_:
		if (pressed) {
			tap_code16(KC_END);
			tap_code16(S(KC_HOME));
			tap_code16(KC_BSPC);
		}
		break;
	}
}


/*
 * Keymaps
 */
const uint16_t PROGMEM keymaps[][MATRIX_ROWS][MATRIX_COLS] = {
#ifdef SEMIMAK
	/*
	 *  Semimax Layer
	 * ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
	 * │  F  │  L  │  H  │  V  │  Z  │  Q  │  W  │  U  │  O  │  Y  │
	 * ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
	 * │  S  │  R  │  N  │  T  │  K  │  C  │  D  │  E  │  A  │  I  │
	 * │ SFT │ CTL │ GUI │ OPT │     │     │ OPT │ GUI │ CTL │ SFT │
	 * └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
	 *    │  X  │  B  │  M  │ ESC │    SPC    │  J  │  P  │  G  │
	 *    │     │     │     │ MD1 │    MD2    │     │     │     │
	 *    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
	 */
	[_SEMIMAK] = LAYOUT(
		KC_F,    KC_L,    KC_H,    KC_V,    KC_Z,    KC_Q,    KC_W,    KC_U,    KC_O,    KC_Y,
		SMMK_S,  SMMK_R,  SMMK_N,  SMMK_T,  KC_K,    KC_C,    SMMK_D,  SMMK_E,  SMMK_A,  SMMK_I,
		    KC_X,    KC_B,    KC_M,    MOD_ESC,      MOD_SP,       KC_J,    KC_P,    KC_G
	),
#endif

#ifdef DVORAK
	/*
	 *  Dvorak Layer
	 * ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
	 * │  Q  │  J  │  K  │  P  │  Y  │  F  │  G  │  C  │  R  │  L  │
	 * ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
	 * │  A  │  O  │  E  │  U  │  I  │  D  │  H  │  T  │  N  │  S  │
	 * │ SFT │ CTL │ GUI │ OPT │     │     │ OPT │ GUI │ CTL │ SFT │
	 * └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
	 *    │  X  │  B  │  M  │ ESC │    SPC    │  W  │  V  │  Z  │
	 *    │     │     │     │ MD1 │    MD2    │     │     │     │
	 *    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
	 */
	[_DVORAK] = LAYOUT(
		KC_Q,    KC_J,    KC_K,    KC_P,    KC_Y,    KC_F,    KC_G,    KC_C,    KC_R,    KC_L,
		DVRK_A,  DVRK_O,  DVRK_E,  DVRK_U,  KC_I,    KC_D,    DVRK_H,  DVRK_T,  DVRK_N,  DVRK_S,
		    KC_X,    KC_B,    KC_M,    MOD_ESC,      MOD_SP,       KC_W,    KC_V,    KC_Z
	),
#endif

#ifdef QWERTY
	/*
	 *  QWERTY(ish) Layer
	 * ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
	 * │  Q  │  W  │  E  │  R  │  T  │  Y  │  U  │  I  │  O  │  P  │
	 * ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
	 * │  A  │  S  │  D  │  F  │  C  │  G  │  H  │  J  │  K  │  L  │
	 * │ SFT │ CTL │ GUI │ OPT │     │     │ OPT │ GUI │ CTL │ SFT │
	 * └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
	 *    │  Z  │  X  │  V  │ ESC │    SPC    │  B  │  N  │  M  │
	 *    │     │     │     │ MD1 │    MD2    │     │     │     │
	 *    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
	 */
	[_QWERTY] = LAYOUT(
	        KC_Q,    KC_W,    KC_E,    KC_R,    KC_T,    KC_Y,    KC_U,    KC_I,    KC_O,    KC_P,    
		QWRT_A,  QWRT_S,  QWRT_D,  QWRT_F,  KC_C,    KC_G,    QWRT_H,  QWRT_J,  QWRT_K,  QWRT_L,  
		    KC_Z,    KC_X,    KC_V,    MOD_ESC,      MOD_SP,       KC_B,    KC_N,    KC_M
	),
#endif

	/*
	 *  MOD1 Layer
	 * ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
	 * │  1  │  2  │  3  │  4  │  5  │  6  │  7  │  8  │  9  │  0  │
	 * ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
	 * │ BSP │ DEL │  [  │  ]  │ TAB │ ENT │  ←  │  ↓  │  ↑  │  →  │
	 * └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
	 *    │  ;  │     │     │  x  │   OTHER   │     │     │     │
	 *    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
	 */
	[MOD1] = LAYOUT(
		KC_1,    KC_2,    KC_3,    KC_4,    KC_5,    KC_6,    KC_7,    KC_8,    KC_9,    KC_0,
		KC_BSPC, KC_DEL,  KC_LBRC, KC_RBRC, KC_TAB,  KC_ENT,  KC_LEFT, KC_DOWN, KC_UP,   KC_RGHT,
		    KC_SCLN, XXXXXXX, XXXXXXX, XXXXXXX,     MO(OTHER),     XXXXXXX, XXXXXXX, XXXXXXX
	),

	/*
	 *  MOD2 Layer
	 * ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
	 * │  F1 │  F2 │  F3 │  F4 │  F5 │  F6 │  F7 │  F8 │  F9 │ F10 │
	 * ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
	 * │  -  │  =  │  /  │  \  │  `  │  '  │  ,  │  .  │ F11 │ F12 │
	 * └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
	 *    │     │     │     │ OTR │     x     │     │     │     │
	 *    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
	 */
	[MOD2] = LAYOUT(
		KC_F1,   KC_F2,   KC_F3,   KC_F4,   KC_F5,   KC_F6,   KC_F7,   KC_F8,   KC_F9,   KC_F10,
		KC_MINS, KC_EQL,  KC_SLSH, KC_BSLS, KC_GRV,  KC_QUOT, KC_COMM, KC_DOT,  KC_F11,  KC_F12,
		    XXXXXXX, XXXXXXX, XXXXXXX, MO(OTHER),     XXXXXXX,     XXXXXXX, XXXXXXX, XXXXXXX
	),

	/*
	 *  OTHER Layer
	 * ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
	 * │     │     │     │     │     │     │     │     │     │     │
	 * ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
	 * │ SMK │ DVK │ QTY │     │     │     │ HOM │ PG↓ │ PG↑ │ END │
	 * └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
	 *    │     │     │     │  x  │     x     │     │     │     │
	 *    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
	 */
	[OTHER] = LAYOUT(
		XXXXXXX, XXXXXXX, XXXXXXX, XXXXXXX, XXXXXXX, XXXXXXX, XXXXXXX, XXXXXXX, XXXXXXX, XXXXXXX,
		LYR_SMK, LYR_DVK, LYR_QTY, XXXXXXX, XXXXXXX, XXXXXXX, KC_HOME, KC_PGDN, KC_PGDN, KC_END,
		    XXXXXXX, XXXXXXX, XXXXXXX, XXXXXXX,      XXXXXXX,      XXXXXXX, XXXXXXX, XXXXXXX
	),
};