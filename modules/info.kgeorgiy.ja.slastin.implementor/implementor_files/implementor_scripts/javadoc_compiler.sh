#!/bin/bash

path_to_rep=../../../java-advanced-2021
path_to_impl=$path_to_rep/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor

javadoc -link https://docs.oracle.com/en/java/javase/14/docs/api/ \
        -cp ".:$path_to_rep/artifacts/info.kgeorgiy.java.advanced.implementor.jar`:
        `$path_to_rep/lib/junit-4.11.jar`:`
        $path_to_rep/lib/quickcheck-0.6.jar" \
        -d ../docs \
        ../info/kgeorgiy/ja/slastin/implementor/*.java  \
        $path_to_impl/Impler.java \
        $path_to_impl/ImplerException.java \
        $path_to_impl/JarImpler.java \
        -private \
        -author
