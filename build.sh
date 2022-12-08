#! /bin/sh

KEYBOARD="alpha"
LAYOUT="nouun"


shopt -s dotglob
set -e


QMK_ARG="compile"

while getopts 'f' flag; do
  case "${flag}" in
    f) QMK_ARG="flash" ;;
  esac
done


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
cp ./layout/* "$QMK_DEST"

# Compile/flash layout
qmk "$QMK_ARG" -kb "$KEYBOARD" -km "$LAYOUT"
