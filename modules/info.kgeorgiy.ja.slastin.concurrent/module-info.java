
module info.kgeorgiy.ja.slastin.concurrent {
    requires transitive info.kgeorgiy.java.advanced.concurrent;
    requires info.kgeorgiy.java.advanced.mapper;

    exports info.kgeorgiy.ja.slastin.concurrent;

    opens info.kgeorgiy.ja.slastin.concurrent to junit;
}

