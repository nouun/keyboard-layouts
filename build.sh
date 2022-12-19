#! /bin/sh

shopt -s dotglob
set -e

usage() {
  echo "usage: $0 [keyboard] [-fh]" 1>&2 
}

exit_abnormal() {
  usage
  exit 1
}

[ $# -eq 0 ] && exit_abnormal

KEYBOARD="$1"
LAYOUT="nouun"
QMK_ARG="compile"

while getopts 'fh' flag  ${@:2}; do
  case "${flag}" in
    f) QMK_ARG="flash" ;;
    h)
      usage
      ;;
    :)
      echo "Error: -${OPTARG} requires an argument."
      exit_abnormal
      ;;
  esac
done

if [ "$KEYBOARD" = "" ]; then
  exit_abnormal
fi

if [ ! -d "./keyboards/$KEYBOARD" ]; then
  echo "invalid keyboard: $KEYBOARD"
  echo "available keyboards: $(ls "./keyboards/")"
  exit 1
fi

QMK_HOME=$(qmk config user.qmk_home)
QMK_HOME=${QMK_HOME#*=}

if [ ! -d "$QMK_HOME/keyboards" ]; then
  echo "           !! Please setup QMK first !!           "
  echo " -> https://docs.qmk.fm/#/newbs_getting_started <-"
  exit
fi

QMK_DEST="$QMK_HOME/keyboards/$KEYBOARD/keymaps/$LAYOUT/"

# Clear old build
[ -d "$QMK_DEST" ] && echo " -- Cleaning old layout" && rm -rf "$QMK_DEST"

# Copy layout
echo " -- Copying new layout"
mkdir -p "$QMK_DEST"
cp "./keyboards/$KEYBOARD/"* "$QMK_DEST"
cp ./shared/* "$QMK_DEST"

# Compile/flash layout
qmk "$QMK_ARG" -kb "$KEYBOARD" -km "$LAYOUT"
