#!/bin/sh

PREFIX=/usr/local

function usage() {
  printf "Usage: %s [-P prefix]\n" $0
}

while getopts hP: OPTION; do
  case $OPTION in
    h) usage; exit 0;;
    P) PREFIX="$OPTARG";;
    ?) usage; exit -1;;
  esac
done
shift `expr $OPTIND - 1`
if  [ ! -z "$*" ]; then
  usage
  exit -1;
fi

install -d $PREFIX/share/gogui/lib
install lib/*.jar $PREFIX/share/gogui/lib

install -d $PREFIX/bin
for f in bin/*; do
  if [ -f $f -a -x $f ]; then
    cat $f \
    | sed "s;GOGUI_LIB=.*;GOGUI_LIB=$PREFIX/share/gogui/lib;" \
    > $PREFIX/$f
    chmod a+x $PREFIX/$f
  fi
done

install -d $PREFIX/share/doc/gogui
install doc/html/*.{html,css} $PREFIX/share/doc/gogui

install -d $PREFIX/share/man/man1
install doc/man/*.1 $PREFIX/share/man/man1
