(header-local base_keymap)

(comment "Handle key presses")

(defmacro check-key [keycode layout]
  (when (= ~keycode keycode)
    (when record->event.pressed
      (set-single-persistent-default-layer ~layout))
    (return false)))

(defn process-layer-switch bool [(keycode u16) ((ptr record) key)]
  (do
    (check-key LYR-CBT -CABBOT)
    (check-key LYR-CNR -CANARY)
    (check-key LYR-SMK -SEMIMAK)
    (check-key LYR-DVK -DVORAK)
    (check-key LYR-QTY -QWERTY)
    (return true)))

(defmacro check-keys [keys]
  (case keycode
    ~keys (return true)))

(defn autoshift-homerow bool [(keycode u16)]
  (do
    (check-keys [CBBT-R CBBT-S CBBT-H CBBT-T CBBT-N CBBT-E CBBT-A CBBT-I]) 
    (check-keys [CNRY-C CNRY-R CNRY-S CNRY-T CNRY-N CNRY-E CNRY-I CNRY-A])
    (check-keys [SMMK-S SMMK-R SMMK-N SMMK-T SMMK-D SMMK-E SMMK-A SMMK-I])
    (check-keys [DVRK-A DVRK-O DVRK-E DVRK-U DVRK-H DVRK-T DVRK-N DVRK-S])
    (check-keys [QWRT-A QWRT-S QWRT-D QWRT-F QWRT-H QWRT-J QWRT-K QWRT-L])
    (return false)))

