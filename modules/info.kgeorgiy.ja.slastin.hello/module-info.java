
module info.kgeorgiy.ja.slastin.hello {
    requires transitive info.kgeorgiy.java.advanced.hello;

    exports info.kgeorgiy.ja.slastin.hello;

    opens info.kgeorgiy.ja.slastin.hello to junit;
}
