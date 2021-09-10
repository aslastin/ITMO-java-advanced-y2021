
module info.kgeorgiy.ja.slastin.implementor {
    requires transitive info.kgeorgiy.java.advanced.implementor;
    requires java.compiler;

    exports info.kgeorgiy.ja.slastin.implementor;

    opens info.kgeorgiy.ja.slastin.implementor to junit;
}
