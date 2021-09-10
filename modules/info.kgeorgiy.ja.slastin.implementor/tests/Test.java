package tests;

import info.kgeorgiy.ja.slastin.implementor.Implementor;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Test {
    // Test1
    public interface A {
        Integer f();
    }

    public interface B {
        Number f();
    }

    public static abstract class Test1CovariantBA implements B, A {
    }

    public static abstract class Test1CovariantAB implements A, B {
    }

    // Test2
    public interface E {
        void f() throws IllegalArgumentException, IOException;
    }

    public interface F {
        void f() throws Exception;
    }

    public static abstract class Test2Exceptions implements F, E {

    }

    // Test3
    public static class ClassD {
        public final Integer f() {
            return null;
        }

    }

    public static class Test3Final extends ClassD implements A {

    }

    // Test4
    public static interface AA {
        static Integer f() {
            return 5;
        }
    }

    public static interface BB {
        Integer f();
    }

    public static abstract class Test4Static implements BB, AA {

    }

    // Test5
    public interface S1 {
    }

    public interface S2 extends S1 {
    }

    public interface S3 extends S2 {
    }

    public interface S4 extends S3 {
    }

    public interface IS1 {
        S1 test();
    }

    public interface IS2 {
        S2 test();
    }

    public interface IS3 {
        S3 test();
    }

    public interface IS4 {
        S4 test();
    }

    public static abstract class Test5AdvancedCovariant implements IS1, IS4, IS2, IS3 {

    }


    public static void test(Path path, Class<?>[] tokens) throws ImplerException, ClassNotFoundException {
        Implementor implementor = new Implementor();
        for (var token : tokens) {
            implementor.implement(token, path);
        }
    }

    public static void main(String[] args) throws ImplerException, ClassNotFoundException {
        Path path = Paths.get("");
        Class<?>[] tokens = {
                Test1CovariantAB.class,
                Test1CovariantBA.class,
                Test2Exceptions.class,
                Test3Final.class,
                Test4Static.class,
                Test5AdvancedCovariant.class
        };
        test(path, tokens);
    }
}
