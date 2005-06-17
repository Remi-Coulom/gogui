#!/bin/sh

# Default prefix should be /usr/local, but some desktop
# environments have problems finding resources there
PREFIX=/usr
JAVA_HOME=java

function usage() {
  printf "Usage: %s [-p prefix][-j javahome]\n" $0
}

while getopts hj:p: OPTION; do
  case $OPTION in
    h) usage; exit 0;;
    j) JAVA_HOME="$OPTARG";;
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
    | sed -e "s;GOGUI_LIB=.*;GOGUI_LIB=$PREFIX/share/gogui/lib;" \
          -e "s;JAVA_DEFAULT=.*;JAVA_DEFAULT=$JAVA_HOME/bin/java;" \
    > $PREFIX/$FILE
    chmod a+x $PREFIX/$FILE
  fi
done

install -d $PREFIX/share/doc/gogui
install doc/manual/html/*.{html,css,png} $PREFIX/share/doc/gogui

install -d $PREFIX/share/man/man1
install doc/manual/man/*.1 $PREFIX/share/man/man1

install -d $PREFIX/share/pixmaps
install src/net/sf/gogui/images/gogui.png src/net/sf/gogui/images/gogui-gnugo.png $PREFIX/share/pixmaps

install -d $PREFIX/share/applications
install config/gogui*.desktop $PREFIX/share/applications

install -d $PREFIX/share/mime/packages
install config/gogui.xml $PREFIX/share/mime/packages

install -d $PREFIX/share/omf/gogui
cat config/gogui.omf \
| sed "s;file:/usr/;file:$PREFIX/;" \
> $PREFIX/share/omf/gogui/gogui.omf

# Update shared mime database and scrollkeeper.
# Fail quietly on error, because they might not be installed
# and are optional.
update-mime-database $PREFIX/share/mime >/dev/null 2>&1
scrollkeeper-update >/dev/null 2>&1
