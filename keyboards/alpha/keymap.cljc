(header-raw QMK-KEYBOARD-H)
(header-local base_keymap)

(comment "Handle key presses")

(defn process-record-user bool [(keycode u16) ((ptr record) key)]
  (return (process-layer-switch keycode record)))


(comment "Enable Auto Shift for homerow mods")

(defn get-custom-auto-shifted-key bool [(keycode u16) ((ptr record) key)]
  (return (autoshift-homerow keycode)))


(comment "Combos")

(def combo-events
  (enum [CMB-EMAIL CMB-TEST COMBO-LENGTH]))

(def (arr combo-email)
  (const u16 PROGMEM [KC-X KC-J COMBO-END]))
(def (arr combo-test)
  (const u16 PROGMEM [KC-X KC-N COMBO-END]))

(def (arr key-combos COMBO-COUNT)
  (var combo-t {CMB-EMAIL (COMBO-ACTION combo-email)
                CMB-TEST  (COMBO-ACTION combo-test)}))

(defn process-combo-event [(combo-index u16) (pressed bool)]
  (case combo-index
    CMB-EMAIL
    (when pressed
      (SEND-STRING "john.doe@example.com"))

    CMB-TEST
    (when pressed
      (tap-code16 KC-END)
      (tap-code16 (S KC-HOME))
      (tap-code16 KC-BSPC))))


(comment "Keymaps")

(def (-> keymaps (arr) (arr MATRIX-ROWS) (arr MATRIX-COLS))
  (const u16 PROGMEM
    {;  Canary Layer
     ; ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
     ; │  W  │  L  │  Y  │  P  │  B  │  Z  │  F  │  O  │  U  │  H  │
     ; ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
     ; │  C  │  R  │  S  │  T  │  G  │  M  │  N  │  E  │  I  │  A  │
     ; │ SFT │ CTL │ GUI │ OPT │     │     │ OPT │ GUI │ CTL │ SFT │
     ; └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
     ;    │  Q  │  J  │  V  │ ESC │    SPC    │  D  │  K  │  X  │
     ;    │     │     │     │ MD1 │    MD2    │     │     │     │
     ;    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
     -CANARY
     (LAYOUT
       KC-W    KC-L    KC-Y    KC-P    KC-B    KC-Z    KC-F    KC-O    KC-U    KC-H   
       CNRY-C  CNRY-R  CNRY-S  CNRY-T  KC-G    KC-M    CNRY-N  CNRY-E  CNRY-I  CNRY-A 
           KC-Q    KC-J    KC-V    MOD-ESC     MOD-SP      KC-D    KC-K    KC-X)

     ;  Semimak Layer
     ; ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
     ; │  F  │  L  │  H  │  V  │  Z  │  Q  │  W  │  U  │  O  │  Y  │
     ; ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
     ; │  S  │  R  │  N  │  T  │  K  │  C  │  D  │  E  │  A  │  I  │
     ; │ SFT │ CTL │ GUI │ OPT │     │     │ OPT │ GUI │ CTL │ SFT │
     ; └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
     ;    │  X  │  B  │  M  │ ESC │    SPC    │  J  │  P  │  G  │
     ;    │     │     │     │ MD1 │    MD2    │     │     │     │
     ;    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
     -SEMIMAK
     (LAYOUT
       KC-F    KC-L    KC-H    KC-V    KC-Z    KC-Q    KC-W    KC-U    KC-O    KC-Y
       SMMK-S  SMMK-R  SMMK-N  SMMK-T  KC-K    KC-C    SMMK-D  SMMK-E  SMMK-A  SMMK-I
           KC-X    KC-B    KC-M    MOD-ESC     MOD-SP      KC-J    KC-P    KC-G)

     ;  Dvorak Layer
     ; ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
     ; │  Q  │  J  │  K  │  P  │  Y  │  F  │  G  │  C  │  R  │  L  │
     ; ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
     ; │  A  │  O  │  E  │  U  │  I  │  D  │  H  │  T  │  N  │  S  │
     ; │ SFT │ CTL │ GUI │ OPT │     │     │ OPT │ GUI │ CTL │ SFT │
     ; └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
     ;    │  X  │  B  │  M  │ ESC │    SPC    │  W  │  V  │  Z  │
     ;    │     │     │     │ MD1 │    MD2    │     │     │     │
     ;    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
     -DVORAK
     (LAYOUT
       KC-Q    KC-J    KC-K    KC-P    KC-Y    KC-F    KC-G    KC-C    KC-R    KC-L
       DVRK-A  DVRK-O  DVRK-E  DVRK-U  KC-I    KC-D    DVRK-H  DVRK-T  DVRK-N  DVRK-S
           KC-X    KC-B    KC-M    MOD-ESC     MOD-SP      KC-W    KC-V    KC-Z)


     ;  QWERTY(ish) Layer
     ; ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
     ; │  Q  │  W  │  E  │  R  │  T  │  Y  │  U  │  I  │  O  │  P  │
     ; ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
     ; │  A  │  S  │  D  │  F  │  C  │  G  │  H  │  J  │  K  │  L  │
     ; │ SFT │ CTL │ GUI │ OPT │     │     │ OPT │ GUI │ CTL │ SFT │
     ; └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
     ;    │  Z  │  X  │  V  │ ESC │    SPC    │  B  │  N  │  M  │
     ;    │     │     │     │ MD1 │    MD2    │     │     │     │
     ;    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
     -QWERTY
     (LAYOUT
       KC-O    KC-P    KC-E    KC-R    KC-T    KC-Y    KC-U    KC-I    KC-O    KC-P
       QWRT-A  QWRT-S  QWRT-D  QWRT-F  KC-C    KC-G    QWRT-H  QWRT-J  QWRT-K  QWRT-L  
           KC-Z    KC-X    KC-V    MOD-ESC     MOD-SP      KC-B    KC-N    KC-M)

     ;  MOD1 Layer
     ; ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
     ; │  1  │  2  │  3  │  4  │  5  │  6  │  7  │  8  │  9  │  0  │
     ; ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
     ; │ BSP │ DEL │  [  │  ]  │ TAB │ ENT │  ←  │  ↓  │  ↑  │  →  │
     ; └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
     ;    │  ;  │     │     │  x  │   OTHER   │     │     │     │
     ;    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
     MOD1
     (LAYOUT
       KC-1    KC-2    KC-3    KC-4    KC-5    KC-6    KC-7    KC-8    KC-9    KC-0
       KC-BSPC KC-DEL  KC-LBRC KC-RBRC KC-TAB  KC-ENT  KC-LEFT KC-DOWN KC-UP   KC-RGHT
           KC-SCLN XXXXXXX XXXXXXX XXXXXXX    (MO OTHER)   XXXXXXX XXXXXXX XXXXXXX)

     ;  MOD2 Layer
     ; ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
     ; │  F1 │  F2 │  F3 │  F4 │  F5 │  F6 │  F7 │  F8 │  F9 │ F10 │
     ; ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
     ; │  -  │  =  │  /  │  \  │  `  │  '  │  ,  │  .  │ F11 │ F12 │
     ; └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
     ;    │     │     │     │ OTR │     x     │     │     │     │
     ;    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
     MOD2
     (LAYOUT
       KC-F1   KC-F2   KC-F3   KC-F4   KC-F5   KC-F6   KC-F7   KC-F8   KC-F9   KC-F10
       KC-MINS KC-EQL  KC-SLSH KC-BSLS KC-GRV  KC-QUOT KC-COMM KC-DOT  KC-F11  KC-F12
           XXXXXXX XXXXXXX XXXXXXX (MO OTHER)   XXXXXXX    XXXXXXX XXXXXXX XXXXXXX)

     ;  OTHER Layer
     ; ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐
     ; │     │     │     │     │     │     │     │     │     │     │
     ; ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤
     ; │ CNR │ SMK │ DVK │ QTY │     │     │ HOM │ PG↓ │ PG↑ │ END │
     ; └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴─────┴──┬──┴──┬──┴──┬──┴──┬──┘
     ;    │     │     │     │  x  │     x     │     │     │     │
     ;    └─────┴─────┴─────┴─────┴───────────┴─────┴─────┴─────┘
     OTHER
     (LAYOUT
       XXXXXXX XXXXXXX XXXXXXX XXXXXXX XXXXXXX XXXXXXX XXXXXXX XXXXXXX XXXXXXX XXXXXXX
       LYR-CNR LYR-SMK LYR-DVK LYR-QTY XXXXXXX XXXXXXX KC-HOME KC-PGDN KC-PGDN KC-END
           XXXXXXX XXXXXXX XXXXXXX XXXXXXX     XXXXXXX     XXXXXXX XXXXXXX XXXXXXX)}))
