# nouun's QMK config

This is a collection of keyboard configurations for various different keyboards
I own and use. They are written in a custom Clojure-esque language which
compiles to C and is then built with QMK.

Supports keyboards are listed under `./keyboards/`.

## Building/Flashing

This uses a custom build tool written in Clojure.

To build a binary:
```sh
clj -M -m build "$KEYBOARD"
```

To build a binary and flash it:
```sh
clj -M -m build "$KEYBOARD" -f
```
or
```sh
./flash.sh "$KEYBOARD"
```
