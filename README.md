## Papyrus-RT Build Fix

This is a set of feature patches for Papyrus-RT (1.0) that enable building and running the generated CDT projects whithin the IDE itself on Linux, Windows, and macOS.

More specifically, these features patches the:
* The code generator plugin to add the missing libraries and libraries paths to the generated CDT project
* The runtime plugin to include pre-build static libraries of the RTS for Linux, macOS, and Windows (Cygwin)
* Updates the CDT core plugin on macOS to resolve CDT bug [519886](https://bugs.eclipse.org/bugs/show_bug.cgi?id=519886) on macOS >= 10.3

## Requirements
Obviously, you will need a C++ compiler to build the generated code:
* On Linux systems, the `g++` and `make` commands should be in your `$PATH`.
* For Windows, [Cygwin](https://www.cygwin.com/) with the `gcc-g++` and `make` packages
* On macOS, [Xcode](https://developer.apple.com/xcode/) or simply the Xcode command line tools which can be installed by running ```xcode-select --install```

## Installation

These patches can be easily installed via the Eclipse update site ```http://jahed.ca/papyrusrt/buildfix```
