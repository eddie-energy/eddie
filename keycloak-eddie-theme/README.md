# Keycloak EDDIE Theme

This directory contains the source code for the keycloak EDDIE Theme.
Currently, the theme only includes some style modifications of the login screen.

## Development Guide

How custom themes have to be structured and a development guide can be found in the
[keycloak docs](https://www.keycloak.org/docs/latest/server_development/#creating-a-theme).

## How to Build

To build a jar file that includes the theme you can either run `jar -cvf eddie-theme.jar .` or you can use the [Makefile](./Makefile).
The Makefile supports the targets `make jar`, which creates the jar file in the current directory and
`make jar-for-aiida`, which creates the jar file in the docker directory of AIIDA.
