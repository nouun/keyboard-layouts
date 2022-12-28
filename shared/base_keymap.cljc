(header-local base_keymap)

(comment "Handle key presses")

(defn process-layer-switch bool [(keycode u16) ((ptr record) key)]
  (case keycode
    LYR-CNR
    (do
      (when record->event.pressed
        (set-single-persistent-default-layer -CANARY))
      (return false))

    LYR-SMK
    (do
      (when record->event.pressed
        (set-single-persistent-default-layer -SEMIMAK))
      (return false))

    LYR-DVK
    (do
      (when record->event.pressed
        (set-single-persistent-default-layer -DVORAK))
      (return false))

    LYR-QTY
    (do
      (when record->event.pressed
        (set-single-persistent-default-layer -QWERTY))
      (return false))
    
    (return true)))

(defn autoshift-homerow bool [(keycode u16)]
  (do
    (case keycode
      [CNRY-C CNRY-R CNRY-S CNRY-T CNRY-N CNRY-E CNRY-I CNRY-A]
      (return true))
    (case keycode
      [SMMK-S SMMK-R SMMK-N SMMK-T SMMK-D SMMK-E SMMK-A SMMK-I]
      (return true))
    (case keycode
      [DVRK-A DVRK-O DVRK-E DVRK-U DVRK-H DVRK-T DVRK-N DVRK-S]
      (return true))
    (case keycode
      [QWRT-A QWRT-S QWRT-D QWRT-F QWRT-H QWRT-J QWRT-K QWRT-L]
      (return true))
    (return false)))

