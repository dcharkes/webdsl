AC_PREREQ([2.60])
AC_INIT([webdsl], [9.7], [webdsl@st.ewi.tudelft.nl])
AC_CONFIG_AUX_DIR([config])
AM_INIT_AUTOMAKE([1.7.2 -Wall -Wno-portability foreign])

m4_pattern_forbid([^(AB|XT)_])
m4_pattern_forbid([^PKG_CHECK_MODULES$])

# set prefix to the default prefix
test "x$prefix" = xNONE && prefix=$ac_default_prefix

### DEPENDENCIES ##################################

XT_SETUP
#XT_USE_BOOTSTRAP_XT_PACKAGES

# comment to make this a stable release
XT_PRE_RELEASE

XT_USE_XT_PACKAGES

## makes the autoconf and svn revision numbers visible through primitives, see stratego sources
XT_TERM_DEFINE

AC_PROG_CC
AC_PROG_INSTALL
PKG_PROG_PKG_CONFIG

AC_DISABLE_STATIC
AC_PROG_LIBTOOL

AC_CHECK_PROG([bash], [bash], [`type -p bash`], [/bin/bash])

WEBDSLC=java

AC_SUBST(WEBDSLC)

### OUTPUT ########################################

AC_CONFIG_FILES([
Makefile
src/Makefile
test/Makefile
src/org/webdsl/dsl/project/testnewproject.sh
src/org/webdsl/dsl/project/webdsl
src/org/webdsl/dsl/project/webdsl-eclipseplugin
src/org/webdsl/dsl/project/webdsl-plugins
src/org/webdsl/dsl/project/Makefile
webdsls.pc
test/run
test/run-test-fail-servlet.sh
test/run-test-fail.sh
test/run-test-succeed-all-servlet.sh
test/run-test-succeed-all.sh
test/run-test-succeed-web-all.sh
test/generatenix.sh
],
[ chmod +x src/org/webdsl/dsl/project/testnewproject.sh
  chmod +x test/run
  chmod +x test/run-test-fail-servlet.sh
  chmod +x test/run-test-fail.sh
  chmod +x test/run-test-succeed-all-servlet.sh
  chmod +x test/run-test-succeed-all.sh
  chmod +x test/run-test-succeed-web-all.sh
  chmod +x test/generatenix.sh
])
AC_OUTPUT

