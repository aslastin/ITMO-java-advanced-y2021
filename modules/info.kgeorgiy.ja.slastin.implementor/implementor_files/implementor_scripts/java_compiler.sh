#!/bin/bash

implementor_jar_path=../../../java-advanced-2021/artifacts/info.kgeorgiy.java.advanced.implementor.jar

javac -cp "$implementor_jar_path" \
        ../info/kgeorgiy/ja/slastin/implementor/*.java \
