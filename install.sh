#!/bin/sh

# Default prefix should be /usr/local, but some desktop
# environments have problems finding resources there
PREFIX=/usr

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
for FILE in bin/*; do
  if [ -f $FILE -a -x $FILE ]; then
    cat $FILE \
    | sed "s;GOGUI_LIB=.*;GOGUI_LIB=$PREFIX/share/gogui/lib;" \
    > $PREFIX/$FILE
    chmod a+x $PREFIX/$FILE
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

install -d $PREFIX/share/mime/packages
install config/gogui.xml $PREFIX/share/mime/packages

install -d $PREFIX/share/omf/gogui
cat config/gogui.omf \
| sed "s;file:/usr/;file:$PREFIX/;" \
> $PREFIX/share/omf/gogui/gogui.omf

update-mime-database $PREFIX/share/mime
scrollkeeper-update
