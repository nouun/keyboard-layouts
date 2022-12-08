
typedef enum {
	TD_NONE,
	TD_UNKNOWN,
	TD_SINGLE_TAP,
	TD_SINGLE_HOLD,
	TD_DOUBLE_TAP,
	TD_DOUBLE_HOLD,
	TD_DOUBLE_SINGLE_TAP, // Send two single taps
	TD_TRIPLE_TAP,
	TD_TRIPLE_HOLD
} td_state_t;

typedef struct {
	bool is_press_action;
	td_state_t state;
} td_tap_t;

// Tap dance enums
enum {
	X_CTL,
	SOME_OTHER_DANCE
};

td_state_t cur_dance(qk_tap_dance_state_t *state);

// For the x tap dance. Put it here so it can be used in any keymap
void x_finished(qk_tap_dance_state_t *state, void *user_data);
void x_reset(qk_tap_dance_state_t *state, void *user_data);


td_state_t cur_dance(qk_tap_dance_state_t *state) {
	if (state->count == 1) {
		if (state->interrupted || !state->pressed) return TD_SINGLE_TAP;
		// Key has not been interrupted, but the key is still held. Means you want to send a 'HOLD'.
		else return TD_SINGLE_HOLD;
	} else if (state->count == 2) {
		// TD_DOUBLE_SINGLE_TAP is to distinguish between typing "pepper", and actually wanting a double tap
		// action when hitting 'pp'. Suggested use case for this return value is when you want to send two
		// keystrokes of the key, and not the 'double tap' action/macro.
		if (state->interrupted) return TD_DOUBLE_SINGLE_TAP;
		else if (state->pressed) return TD_DOUBLE_HOLD;
		else return TD_DOUBLE_TAP;
	}

	// Assumes no one is trying to type the same letter three times (at least not quickly).
	// If your tap dance key is 'KC_W', and you want to type "www." quickly - then you will need to add
	// an exception here to return a 'TD_TRIPLE_SINGLE_TAP', and define that enum just like 'TD_DOUBLE_SINGLE_TAP'
	if (state->count == 3) {
		if (state->interrupted || !state->pressed) return TD_TRIPLE_TAP;
		else return TD_TRIPLE_HOLD;
	} else return TD_UNKNOWN;
}

// Create an instance of 'td_tap_t' for the 'x' tap dance.
static td_tap_t xtap_state = {
	.is_press_action = true,
	.state = TD_NONE
};

void x_finished(qk_tap_dance_state_t *state, void *user_data) {
	xtap_state.state = cur_dance(state);
	switch (xtap_state.state) {
		case TD_SINGLE_TAP: register_code(KC_X); break;
		case TD_SINGLE_HOLD: register_code(KC_LCTL); break;
		case TD_DOUBLE_TAP: register_code(KC_ESC); break;
		case TD_DOUBLE_HOLD: register_code(KC_LALT); break;
		// Last case is for fast typing. Assuming your key is `f`:
		// For example, when typing the word `buffer`, and you want to make sure that you send `ff` and not `Esc`.
		// In order to type `ff` when typing fast, the next character will have to be hit within the `TAPPING_TERM`, which by default is 200ms.
		case TD_DOUBLE_SINGLE_TAP: tap_code(KC_X); register_code(KC_X); break;
		default: break;
	}
}

void x_reset(qk_tap_dance_state_t *state, void *user_data) {
	switch (xtap_state.state) {
		case TD_SINGLE_TAP: unregister_code(KC_X); break;
		case TD_SINGLE_HOLD: unregister_code(KC_LCTL); break;
		case TD_DOUBLE_TAP: unregister_code(KC_ESC); break;
		case TD_DOUBLE_HOLD: unregister_code(KC_LALT); break;
		case TD_DOUBLE_SINGLE_TAP: unregister_code(KC_X); break;
		default: break;
	}
	xtap_state.state = TD_NONE;
}
