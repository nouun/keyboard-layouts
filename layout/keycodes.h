/*
 * Homerow Mods
 */

// Left hand Semimok
#define HOME_S LSFT_T(KC_S)
#define HOME_R LCTL_T(KC_R)
#define HOME_N LGUI_T(KC_N)
#define HOME_T LALT_T(KC_T)

// Right hand Semimok
#define HOME_D RALT_T(KC_D)
#define HOME_E RGUI_T(KC_E)
#define HOME_A RCTL_T(KC_A)
#define HOME_I LSFT_T(KC_I)

/*
 * Custom Keys
 */
enum custom_keycodes {
	// Double tap space for peried
	SPC_PERIOD,
};
//
#define MOD_ESC LT(MOD1, KC_ESC)
#define MOD_SP  MT(MOD_LSFT, SPC_PERIOD)
