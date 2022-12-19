/*
 * Homerow Mods
 */

#ifdef SEMIMAK
// Left hand Semimok
#define SMMK_S LSFT_T(KC_S)
#define SMMK_R LCTL_T(KC_R)
#define SMMK_N LGUI_T(KC_N)
#define SMMK_T LALT_T(KC_T)

// Right hand Semimok
#define SMMK_D RALT_T(KC_D)
#define SMMK_E RGUI_T(KC_E)
#define SMMK_A RCTL_T(KC_A)
#define SMMK_I RSFT_T(KC_I)
#endif

#ifdef DVORAK
// Left hand Dvorak
#define DVRK_A LSFT_T(KC_A)
#define DVRK_O LCTL_T(KC_O)
#define DVRK_E LGUI_T(KC_E)
#define DVRK_U LALT_T(KC_U)

// Right hand Dvorak
#define DVRK_H RALT_T(KC_H)
#define DVRK_T RGUI_T(KC_T)
#define DVRK_N RCTL_T(KC_N)
#define DVRK_S RSFT_T(KC_S)
#endif

#ifdef QWERTY
// Left hand QWERTY
#define QWRT_A LSFT_T(KC_A)
#define QWRT_S LCTL_T(KC_S)
#define QWRT_D LGUI_T(KC_D)
#define QWRT_F LALT_T(KC_F)

// Right hand QWERTY
#define QWRT_H RALT_T(KC_H)
#define QWRT_J RALT_T(KC_J)
#define QWRT_K RGUI_T(KC_K)
#define QWRT_L RCTL_T(KC_L)
#endif

/*
 * Custom Keys
 */

#define MOD_ESC LT(MOD1, KC_ESC)
#define MOD_SP  LT(MOD2, KC_SPC)

