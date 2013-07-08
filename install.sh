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

install -d "$PREFIX/share/gogui/lib"
install -m 644 lib/*.jar "$PREFIX/share/gogui/lib"

# Install files to $PREFIX/bin

JAVA_DEFAULT="$JAVA_HOME/bin/java"
install -d "$PREFIX/bin"
for FILE in bin/*; do
    if [ -f $FILE -a -x $FILE ]; then
        cat $FILE \
        | sed -e "s;^GOGUI_LIB=.*;GOGUI_LIB=\"$PREFIX/share/gogui/lib\";" \
              -e "s;^JAVA_DEFAULT=.*;JAVA_DEFAULT=\"$JAVA_DEFAULT\";" \
        > "$PREFIX/$FILE"
        chmod a+x "$PREFIX/$FILE"
    fi
done

# Install files to $PREFIX/share/doc/gogui

install -d "$PREFIX/share/doc/gogui"
install -m 644 doc/manual/html/*.html "$PREFIX/share/doc/gogui"

# Install files to $PREFIX/share/man

install -d "$PREFIX/share/man/man1"
install -m 644 doc/manual/man/*.1 "$PREFIX/share/man/man1"

# Install icons

install -d "$PREFIX/share/icons/hicolor/48x48/apps"
install -m 644 src/net/sf/gogui/images/gogui-48x48.png \
    "$PREFIX/share/icons/hicolor/48x48/apps/gogui.png"
install -d "$PREFIX/share/icons/hicolor/scalable/apps"
install -m 644 src/net/sf/gogui/images/gogui.svg \
    "$PREFIX/share/icons/hicolor/scalable/apps"
install -d "$PREFIX/share/icons/hicolor/48x48/mimetypes"
install -m 644 config/application-x-go-sgf.png \
    "$PREFIX/share/icons/hicolor/48x48/mimetypes"

# Install desktop entry

install -d "$PREFIX/share/applications"
install -m 644 config/gogui.desktop "$PREFIX/share/applications"

# Install shared mime info

install -d "$PREFIX/share/mime/packages"
install -m 644 config/gogui-mime.xml "$PREFIX/share/mime/packages"

# Install Gnome 2 thumbnailer

install -d "$SYSCONFDIR/gconf/schemas"
cat config/gogui.schemas \
| sed "s;/usr/bin/gogui-thumbnailer;$PREFIX/bin/gogui-thumbnailer;" \
> "$SYSCONFDIR/gconf/schemas/gogui.schemas"

# Install Gnome 3 thumbnailer

install -d "$PREFIX/share/thumbnailers"
cat config/gogui.thumbnailer \
| sed "s;/usr/bin/gogui-thumbnailer;$PREFIX/bin/gogui-thumbnailer;" \
> "$PREFIX/share/thumbnailers/gogui.thumbnailer"

#-----------------------------------------------------------------------------
# Post installation
# Fail quietly on error, some programs might not be available
#-----------------------------------------------------------------------------

# Gnome thumbnailer

export GCONF_CONFIG_SOURCE=`gconftool-2 --get-default-source`
gconftool-2 --makefile-install-rule \
    "$SYSCONFDIR/gconf/schemas/gogui.schemas" >/dev/null 2>&1

# Desktop database

update-desktop-database "$PREFIX/share/applications" >/dev/null 2>&1

# MIME database

update-mime-database "$PREFIX/share/mime" >/dev/null 2>&1
