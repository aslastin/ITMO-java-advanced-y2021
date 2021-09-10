#!/bin/bash

implementor_jar_path=../../../java-advanced-2021/artifacts/info.kgeorgiy.java.advanced.implementor.jar
module_path=../../../java-advanced-2021/artifacts:../../../java-advanced-2021/lib

javac -d . \
    -cp "$implementor_jar_path" \
    --module-path $module_path \
    ../info/kgeorgiy/ja/slastin/implementor/*.java ../module-info.java

jar -c -f Implementor.jar -m ../META-INF/MANIFEST.MF .
rm -r info
rm module-info.class
