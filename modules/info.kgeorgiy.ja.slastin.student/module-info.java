
module info.kgeorgiy.ja.slastin.student {
    requires transitive info.kgeorgiy.java.advanced.student;

    exports info.kgeorgiy.ja.slastin.student;

    opens info.kgeorgiy.ja.slastin.student to junit;
}
