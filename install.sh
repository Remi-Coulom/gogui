#!/bin/sh

PREFIX=/usr/local

function usage() {
  printf "Usage: %s [-p prefix]\n" $0
}

while getopts hp: OPTION; do
  case $OPTION in
    h) usage; exit 0;;
    p) PREFIX="$OPTARG";;
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

install -d $PREFIX/share/pixmaps
install src/images/gogui.png $PREFIX/share/pixmaps

install -d $PREFIX/share/applications
install config/gogui.desktop $PREFIX/share/applications

install -d $PREFIX/share/mime-info
install config/gogui.{mime,keys} $PREFIX/share/mime-info

install -d $PREFIX/share/application-registry
install config/gogui.applications $PREFIX/share/application-registry
