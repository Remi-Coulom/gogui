#!/bin/sh

# Default prefix should be /usr/local, but some desktop
# environments have problems finding resources there

PREFIX=/usr
SYSCONFDIR=/etc
JAVA_HOME=

function usage() {
    printf "Usage: %s [-p prefix] [-j javahome]\n" $0
}

#-----------------------------------------------------------------------------
# Parse options
#-----------------------------------------------------------------------------

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

if [ -z "$JAVA_HOME" ]; then
    echo "Use option -j to specify the installation directory of a" >&2
    echo "Java 1.4 compatible virtual machine" >&2
    exit -1
fi
if [ ! -x "$JAVA_HOME/bin/java" ]; then
    echo "$JAVA_HOME/bin/java does not exist or is not executable" >&2
    exit -1
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
install -m 644 doc/manual/html/*.{html,png} $PREFIX/share/doc/gogui

# Install files to $PREFIX/share/man

install -d $PREFIX/share/man/man1
install -m 644 doc/manual/man/*.1 $PREFIX/share/man/man1

# Install icons

for SIZE in 16 32 48 64 128; do
    install -d $PREFIX/share/icons/hicolor/"$SIZE"x"$SIZE"/apps
    install -m 644 src/net/sf/gogui/images/gogui-"$SIZE"x"$SIZE".png \
        $PREFIX/share/icons/hicolor/"$SIZE"x"$SIZE"/apps/gogui.png
done
# hicolor is the standard according to freedesktop.org, but for compatibility
# we also install the icon to pixmaps
install -d $PREFIX/share/pixmaps
install -m 644 src/net/sf/gogui/images/gogui-48x48.png \
    $PREFIX/share/pixmaps/gogui.png

# Install desktop entry

install -d $PREFIX/share/applications
install -m 644 config/gogui.desktop $PREFIX/share/applications
# Add DocPath entry used by KDE 3.4
echo "DocPath=file:$PREFIX/share/doc/gogui/index.html" \
    >> $PREFIX/share/applications/gogui.desktop

# Install shared mime info

install -d $PREFIX/share/mime/packages
install -m 644 config/gogui.xml $PREFIX/share/mime/packages

# Install mime icon

install -d $PREFIX/share/icons/hicolor/48x48/mimetypes
install -m 644 config/gogui-application-x-go-sgf.png \
  $PREFIX/share/icons/hicolor/48x48/mimetypes

# Install KDE mime entry
# Could create a conflict with other packages.
# Remove when KDE supports the standard shared MIME database

install -d $PREFIX/share/mimelnk/application
install -m 644 config/x-go-sgf.desktop $PREFIX/share/mimelnk/application

# Install Gnome thumbnailer

install -d $SYSCONFDIR/gconf/schemas
cat config/gogui.schemas \
| sed "s;/usr/bin/sgfthumbnail;$PREFIX/bin/sgfthumbnail;" \
> $SYSCONFDIR/gconf/schemas/gogui.schemas

# Install scrollkeeper entry

install -d $PREFIX/share/omf/gogui
cat config/gogui.omf \
| sed "s;file:/usr/;file:$PREFIX/;" \
> $PREFIX/share/omf/gogui/gogui.omf

#-----------------------------------------------------------------------------
# Post installation
# Fail quietly on error, some programs might not be available
#-----------------------------------------------------------------------------

# Update shared mime/desktop databases and scrollkeeper.

update-mime-database $PREFIX/share/mime >/dev/null 2>&1
update-desktop-database $PREFIX/share/applications >/dev/null 2>&1
scrollkeeper-update >/dev/null 2>&1

# Gnome thumbnailer

export GCONF_CONFIG_SOURCE=`gconftool-2 --get-default-source`
gconftool-2 --makefile-install-rule \
    /$SYSCONFDIR/gconf/schemas/gogui.schemas >/dev/null 2>&1
