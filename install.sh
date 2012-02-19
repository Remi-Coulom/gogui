#!/bin/sh

PREFIX=/usr/local
JAVA_HOME=/usr

usage() {
    printf "Usage: %s -j javahome [-p prefix] [-s sysconfdir]\n" $0
}

#-----------------------------------------------------------------------------
# Parse options
#-----------------------------------------------------------------------------

while getopts hj:p:s: OPTION; do
    case $OPTION in
        h) usage; exit 0;;
        j) JAVA_HOME="$OPTARG";;
        p) PREFIX="$OPTARG";;
        s) SYSCONFDIR="$OPTARG";;
        ?) usage; exit 1;;
    esac
done
shift `expr $OPTIND - 1`
if  [ ! -z "$*" ]; then
    usage
    exit 1;
fi

if [ -z "$JAVA_HOME" ]; then
    echo "Use option -j to specify the installation directory of a" >&2
    echo "Java 1.5 compatible virtual machine" >&2
    exit 1
fi
if [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "$JAVA_HOME/bin/java does not exist or is not executable" >&2
    exit 1
fi
if [ -z "$SYSCONFDIR" ]; then
    SYSCONFDIR="$PREFIX/etc"
fi

#-----------------------------------------------------------------------------
# Install files
#-----------------------------------------------------------------------------

# Install files to $PREFIX/share/gogui/lib

install -d $PREFIX/share/gogui/lib
install -m 644 lib/*.jar $PREFIX/share/gogui/lib

# Install files to $PREFIX/bin

JAVA_DEFAULT="$JAVA_HOME/bin/java"
install -d $PREFIX/bin
for FILE in bin/*; do
    if [ -f $FILE -a -x $FILE ]; then
        cat $FILE \
        | sed -e "s;^GOGUI_LIB=.*;GOGUI_LIB=\"$PREFIX/share/gogui/lib\";" \
              -e "s;^JAVA_DEFAULT=.*;JAVA_DEFAULT=\"$JAVA_DEFAULT\";" \
        > $PREFIX/$FILE
        chmod a+x $PREFIX/$FILE
    fi
done

# Install files to $PREFIX/share/doc/gogui

install -d $PREFIX/share/doc/gogui
install -m 644 doc/manual/html/*.html $PREFIX/share/doc/gogui

# Install files to $PREFIX/share/man

install -d $PREFIX/share/man/man1
install -m 644 doc/manual/man/*.1 $PREFIX/share/man/man1

# Install icons

xdg-icon-resource install --size 48 config/gogui-gogui.png
xdg-icon-resource install --size 48 --context mimetypes \
    config/application-x-go-sgf.png

# Install desktop entry

xdg-desktop-menu install config/gogui-gogui.desktop

# Install shared mime info

xdg-mime install config/gogui-mime.xml

# Install Gnome 2 thumbnailer

install -d $SYSCONFDIR/gconf/schemas
cat config/gogui.schemas \
| sed "s;/usr/bin/gogui-thumbnailer;$PREFIX/bin/gogui-thumbnailer;" \
> $SYSCONFDIR/gconf/schemas/gogui.schemas

# Install Gnome 3 thumbnailer

install -d $PREFIX/share/thumbnailers
cat config/gogui.thumbnailer \
| sed "s;/usr/bin/gogui-thumbnailer;$PREFIX/bin/gogui-thumbnailer;" \
> $PREFIX/share/thumbnailers/gogui.thumbnailer

#-----------------------------------------------------------------------------
# Post installation
# Fail quietly on error, some programs might not be available
#-----------------------------------------------------------------------------

# Gnome thumbnailer

export GCONF_CONFIG_SOURCE=`gconftool-2 --get-default-source`
gconftool-2 --makefile-install-rule \
    $SYSCONFDIR/gconf/schemas/gogui.schemas >/dev/null 2>&1
