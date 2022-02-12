package info.kgeorgiy.ja.slastin.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class Implementor implements JarImpler {
    /**
     * Tab for implemented classes
     */
    private final static String TAB = "    ";
    /**
     * Used as a line separator in implemented class
     */
    private final static String LINE_SEPARATOR = System.lineSeparator();
    /**
     * Filename extension for source java files
     */
    private final static String JAVA = ".java";
    /**
     * Filename extension for compiled java files
     */
    private final static String CLASS = ".class";

    /**
     * Used in {@link #methodHashCode(Method)} as a <em>base</em>
     */
    private final static int X = 29;
    /**
     * Used in {@link #methodHashCode(Method)} as a <em>module</em>
     */
    private final static int MOD = 40960001;
    /**
     * Predicate which returns true if only given method isn't static or final
     */
    private static final Predicate<Method> FINAL_OR_STATIC_PREDICATE =
            method -> Modifier.isFinal(method.getModifiers()) || Modifier.isStatic(method.getModifiers());
    /**
     * Comparator which compare classes using {@link Class#isAssignableFrom(Class)}.
     *
     * <p>Consider the possible options:
     *  <ul>
     *  <li> {@code clazz1 < clazz2 <=> clazz1.isAssignableFrom(clazz2) == true && clazz2.isAssignableFrom(clazz1) == false}</li>
     *  <li> {@code clazz1 > clazz2 <=> clazz1.isAssignableFrom(clazz2) == false}
     *  <li> {@code class1 == class2 <=> clazz1.isAssignableFrom(clazz2) == true && clazz2.isAssignableFrom(clazz1) == true}
     *  </ul>
     * Null values aren't supported
     */
    private static final Comparator<Class<?>> CLASS_NO_NULL_COMPARATOR = (clazz1, clazz2) -> {
        if (clazz1.isAssignableFrom(clazz2)) {
            if (clazz2.isAssignableFrom(clazz1)) {
                return 0;
            }
            return -1;
        }
        return 1;
    };
    /**
     * Used for deleting files in temporary directory.
     *
     * @see SimpleFileVisitor
     */
    private static final SimpleFileVisitor<Path> DELETE_VISITOR = new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    /**
     * Generates a hashCode for provided method relying on {@link Method#getParameterTypes()} and {@link Method#getName()}
     *
     * @param method method for which hashCode will be created
     * @return hashCode of the provided method
     */
    private static int methodHashCode(Method method) {
        return (Arrays.hashCode(method.getParameterTypes()) * X * X + method.getName().hashCode() * X) % MOD;
    }

    /**
     * Checks that provided methods are equal relying on {@link Method#getName()} and {@link Method#getParameterTypes()}.
     * Support null conditions of provided methods.
     *
     * @param method1 first method for compare
     * @param method2 second method for compare
     * @return true if methods are equal, false otherwise
     */
    private static boolean methodEquals(Method method1, Method method2) {
        if (method1 == method2) {
            return true;
        }
        if (method1 == null || method2 == null) {
            return false;
        }
        return method1.getName().equals(method2.getName()) &&
                Arrays.equals(method1.getParameterTypes(), method2.getParameterTypes());
    }

    /**
     * Delete all content of provided root using {@link #DELETE_VISITOR}
     *
     * @param root root to delete
     * @throws IOException If errors occur during deleting root content
     */
    private static void clean(final Path root) throws IOException {
        Files.walkFileTree(root, DELETE_VISITOR);
    }

    /**
     * @param token used for creating result predicate
     * @return {@link Predicate} which returns true if only the given method is:
     * <ul>
     * <li> not Synthetic {@link Method#isSynthetic()}</li>
     * <li> not Bridge {@link Method#isBridge()}</li>
     * <li> not final </li>
     * <li> not private </li>
     * <li> not static </li>
     * <li> package-private and has the same package as a provided token </li>
     * </ul>
     */
    private static Predicate<Method> getMethodPredicate(Class<?> token) {
        String tokenPackage = token.getPackageName();
        return method -> {
            int mod = method.getModifiers();
            return !method.isSynthetic() &&
                    !method.isBridge() &&
                    !Modifier.isFinal(mod) &&
                    !Modifier.isStatic(mod) &&
                    !Modifier.isPrivate(mod) &&
                    (Modifier.isPublic(mod) || Modifier.isProtected(mod) ||
                            method.getDeclaringClass().getPackageName().equals(tokenPackage));
        };
    }

    /**
     * Return class name of the implemented file.
     * The code inside is equal to
     * <pre>{@code return token.getSimpleName() + "Impl"}</pre>
     *
     * @param token with help of whom generated class name will be provided
     * @return {@link String} representing class name
     */
    private static String implClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Returns implemented file name based on provided token package, given separator and file extension.
     * The code inside is equal to
     * <pre>{@code return (token.getPackageName() + "." +
     *                      implClassName(token)).replace(".", separator) + suffix;}</pre>
     *
     * @param token     provided token with help of whom file name will be generated
     * @param separator used as a path delimiter in file name
     * @param suffix    extension of result file
     * @return {@link String} respresenting token file name
     */
    private static String getTokenFileName(Class<?> token, String separator, String suffix) {
        return (token.getPackageName() + "." + implClassName(token)).replace(".", separator) + suffix;
    }

    /**
     * Returns path to file, containing implementation of the given class, with specific file extension
     * located in directory represented by path
     *
     * @param root   path to parent directory of class
     * @param token  class to get name from
     * @param suffix file extension
     * @return {@link Path} representing path to certain file
     */
    private static Path getTokenFilePath(Path root, Class<?> token, String suffix) {
        return root.resolve(getTokenFileName(token, File.separator, suffix)).toAbsolutePath();
    }

    /**
     * Creates parent directory for the file represented by path
     *
     * @param path file to create parent directory
     * @throws ImplerException if error occurred during creating process
     */
    private static void createDirectories(Path path) throws ImplerException {
        Path parent;
        if ((parent = path.getParent()) == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new ImplerException("Can not create overlying directories for impl class");
        }
    }

    /**
     * Returns a class path of the provided class
     *
     * @param token class to get class path from
     * @return {@link String} class path to provided token
     */
    private static String getClassPath(Class<?> token) {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Gets default value of the given method return type
     *
     * @param method method to get default value from it's return type
     * @return {@link String} representing value
     */
    private static String getDefaultReturnValue(Method method) {
        Class<?> clazz = method.getReturnType();
        if (clazz.equals(void.class)) {
            return "";
        }
        if (clazz.equals(boolean.class)) {
            return " false";
        }
        if (clazz.isPrimitive()) {
            return " 0";
        }
        return " null";
    }

    /**
     * Returns parameters of the provided executable
     *
     * @param executable executable to get parameters from
     * @return {@link String} consists of string representation of executable parameters
     */
    private static String implParameters(Executable executable) {
        var parameterTypes = executable.getParameterTypes();
        return IntStream.range(0, parameterTypes.length)
                .mapToObj(i -> parameterTypes[i].getCanonicalName() + " arg" + (i + 1))
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Returns an exception signature of the provided executable.
     * If executable has no exceptions returns an empty string.
     *
     * @param executable executable to get exceptions from
     * @return {@link String} consists of string representation of executable exception signature
     */
    private static String implExceptions(Executable executable) {
        Class<?>[] exceptionTypes = executable.getExceptionTypes();
        if (exceptionTypes.length > 0) {
            return Arrays.stream(exceptionTypes)
                    .map(Class::getCanonicalName)
                    .collect(Collectors.joining(", ", " throws ", ""));
        }
        return "";
    }

    /**
     * Returns string consists of implemented executable
     *
     * @param modifiers   used as modifiers of the executable
     * @param returnValue used as return value of the executable
     * @param name        used as a name of the executable
     * @param parameters  used as parameters of the executable
     * @param exceptions  used as an exception signature of the executable
     * @param body        used as a body of the executable
     * @return {@link String} representing implemented executable
     */
    private static String implExecutable(String modifiers, String returnValue, String name,
                                         String parameters, String exceptions, String body) {
        return TAB +
                modifiers +
                returnValue +
                ' ' +
                name +
                parameters +
                exceptions +
                " {" + LINE_SEPARATOR + TAB + TAB +
                body + ";" + LINE_SEPARATOR + TAB +
                "}" + LINE_SEPARATOR + LINE_SEPARATOR;
    }

    /**
     * Returns implemented constructor
     *
     * @param constructor to implemented
     * @return {@link String} consists of implemented give constuctor
     */
    private static String implConstructor(Constructor<?> constructor) {
        String args = IntStream.range(0, constructor.getParameterCount())
                .mapToObj(i -> "arg" + (i + 1))
                .collect(Collectors.joining(", ", "(", ")"));

        return implExecutable(
                Modifier.toString(constructor.getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)),
                "",
                implClassName(constructor.getDeclaringClass()),
                implParameters(constructor),
                implExceptions(constructor),
                "super" + args
        );
    }

    /**
     * Gets package of given token.
     * Package is empty, if class is situated in default package.
     *
     * @param token class to get package
     * @return {@link String} representing package
     */
    private static String implPackage(Class<?> token) {
        return String.format("package %s;%n%n", token.getPackageName());
    }

    /**
     * Returns implemented class signature base on provided token
     *
     * @param token with help of whom implementation will be generated
     * @return {@link String} consists of class signature
     */
    private static String implClassSignature(Class<?> token) {
        return String.format("public class %s %s %s",
                implClassName(token),
                (token.isInterface() ? "implements" : "extends"),
                token.getCanonicalName()
        );
    }

    /**
     * Implement all non-private constructors of provided token
     *
     * @param token whom constructors will be implemented
     * @return {@link String} all implemented non-private constructors of provided token
     * @throws ImplerException if all constructors of provided token are private
     */
    private static String implConstructors(Class<?> token) throws ImplerException {
        if (token.isInterface()) {
            return "";
        }

        List<Constructor<?>> constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .collect(Collectors.toList());

        if (constructors.size() == 0) {
            throw new ImplerException("No non-private constructors in input class");
        }

        return constructors.stream().map(Implementor::implConstructor).collect(Collectors.joining());
    }

    /**
     * Do DFS by token inheritance tree including interfaces and classes in it.
     *
     * @param token         with that token DFS on the inheritance tree will be started
     * @param visitedTokens contains all visited classes in inheritance token
     * @param methods       contains all methods of provided token including all classes in inheritance tree
     */
    private static void methodDFS(Class<?> token, Set<Class<?>> visitedTokens, List<Method> methods) {
        if (token == null || visitedTokens.contains(token)) {
            return;
        }
        visitedTokens.add(token);
        methods.addAll(Arrays.asList(token.getDeclaredMethods()));
        for (var currentToken : token.getInterfaces()) {
            methodDFS(currentToken, visitedTokens, methods);
        }
        methodDFS(token.getSuperclass(), visitedTokens, methods);
    }

    /**
     * Returns all available for implementation methods of token
     *
     * @param token methods of whom will be implemented
     * @return {@link String} representing all implemented methods of the provided token
     */
    private static String implMethods(Class<?> token) {
        Predicate<Method> methodPredicate = getMethodPredicate(token);

        List<Method> methods = new ArrayList<>();
        methodDFS(token, new HashSet<>(), methods);

        return methods.stream()
                .collect(Collectors.groupingBy(Implementor::methodHashCode))
                .values().stream()
                .flatMap(Implementor::getStreamOfEqualMethods)
                .map(equalMethods -> implMethod(equalMethods, methodPredicate))
                .collect(Collectors.joining());
    }

    /**
     * In this method delivered all methods with same hashCode got by {@link #methodHashCode(Method)}
     * Returns stream of list in which lying only methods satisfying {@link #methodEquals(Method, Method)} condition
     *
     * @param methods methods with same hashCode by {@link #methodHashCode(Method)}
     * @return {@link Stream} containing list of method equal by {@link #methodEquals(Method, Method)}
     */
    private static Stream<List<Method>> getStreamOfEqualMethods(List<Method> methods) {
        List<List<Method>> equalMethods = new ArrayList<>();
        for (var method : methods) {
            int i = 0;
            while (i < equalMethods.size() && !methodEquals(method, equalMethods.get(i).get(0))) {
                ++i;
            }
            if (i == equalMethods.size()) {
                equalMethods.add(new ArrayList<>(Arrays.asList(method)));
            } else {
                equalMethods.get(i).add(method);
            }
        }
        return equalMethods.stream();
    }

    /**
     * Returns implementation of the method based on methods.
     * Supports covariant return type, exceptions with different signature. Result method will have <em>public</em> modifier.
     *
     * @param methods         methods who are equal by {@link #methodEquals(Method, Method)}
     * @param methodPredicate predicate with help of whom methods will be filtered
     * @return {@link String} representing implemented method or empty string if method can not be implemented
     */
    private static String implMethod(List<Method> methods, Predicate<Method> methodPredicate) {
        if (methods.isEmpty() ||
                methods.stream().anyMatch(FINAL_OR_STATIC_PREDICATE) ||
                methods.stream().noneMatch(methodPredicate)) {
            return "";
        }

        String returnType = methods.stream()
                .map(Method::getReturnType)
                .max(CLASS_NO_NULL_COMPARATOR)
                .map(Class::getCanonicalName)
                .orElse("");

        String exceptions;
        Method method = methods.get(0);
        Class<?>[] arrayOfExceptions = method.getExceptionTypes();
        if (methods.stream().map(Method::getExceptionTypes).allMatch(e -> Arrays.equals(e, arrayOfExceptions))) {
            exceptions = implExceptions(method);
        } else {
            exceptions = "";
        }

        return implExecutable(
                "public ",
                returnType,
                method.getName(),
                implParameters(method),
                exceptions,
                "return" + getDefaultReturnValue(method)
        );
    }

    /**
     * Writes args with help of writer using Unicode format
     *
     * @param writer write given arguments to itself
     * @param args   arguments to be written
     * @throws IOException if error occur during {@code writer.write(...)}
     */
    private static void writeAll(Writer writer, String... args) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            for (char ch : arg.toCharArray()) {
                sb.append((ch >= 128) ? String.format("\\u%04x", (int) ch) : ch);
            }
        }
        writer.write(sb.toString());
    }

    /**
     * Check that token satisfy given predicate
     *
     * @param token     token for predicate check
     * @param predicate predicate applied for to the token
     * @param message   information about occurred error
     * @throws ImplerException if {@code predicate.test(token) == false}
     */
    private static void checkCondition(Class<?> token, Predicate<Class<?>> predicate, String message) throws ImplerException {
        if (predicate.test(token)) {
            throw new ImplerException("Incorrect token type: token can not be " + message);
        }
    }

    /**
     * Checks that token can be implemented by {@link Implementor}
     *
     * @param token class that will be checked
     * @throws ImplerException if given token is :
     *                         <ol>
     *                             <li>primitive type</li>
     *                             <li>array type</li>
     *                             <li>{@link Enum}</li>
     *                             <li>final class</li>
     *                             <li>private class</li>
     *                         </ol>
     */
    private static void checkToken(Class<?> token) throws ImplerException {
        checkCondition(token, Class::isPrimitive, "primitive");
        checkCondition(token, Class::isArray, "array");
        checkCondition(token, Predicate.isEqual(Enum.class), "enum");
        checkCondition(token, (clazz) -> Modifier.isFinal(clazz.getModifiers()), "final");
        checkCondition(token, (clazz) -> Modifier.isPrivate(clazz.getModifiers()), "private");
    }

    /**
     * Check that all provided objects aren't null.
     *
     * @param message the message that will be inside {@link ImplerException}
     * @param objects objects to be checked for the null equality
     * @throws ImplerException If some objects are null
     */
    private static void assertNotNull(String message, Object... objects) throws ImplerException {
        for (Object object : objects) {
            if (object == null) {
                throw new ImplerException(message);
            }
        }
    }

    /**
     * Check that all provided objects are equal using {@link Object#equals(Object)}
     *
     * @param message the message that will be inside {@link ImplerException}
     * @param objects objects to be checked for the equality
     * @throws ImplerException If some objects aren't equal
     */
    private static void assertEquals(String message, Object... objects) throws ImplerException {
        for (var object : objects) {
            if (!object.equals(objects[0])) {
                throw new ImplerException(message);
            }
        }
    }

    /**
     * Runs {@link Implementor} in different ways.
     * There are 2 possible ways:
     *  <ul>
     *  <li> 2 arguments: className rootPath - runs {@link #implement(Class, Path)} with given arguments</li>
     *  <li> 3 arguments: -jar className jarPath - runs {@link #implementJar(Class, Path)} with last two arguments</li>
     *  </ul>
     * <p>
     * Method prints message with information about arisen problem,
     * if any arguments are invalid or an error occurs during implementation.
     *
     * @param args arguments for running an application
     */
    public static void main(String[] args) {
        if (args == null || !(args.length == 2 || args.length == 3)) {
            System.err.println("Expected two or three arguments");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.err.println("All args must be no null");
                return;
            }
        }
        try {
            JarImpler implementor = new Implementor();
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (ImplerException e) {
            System.err.println("Can not implement input class: " + e);
        } catch (ClassNotFoundException e) {
            System.err.println("Input class wasn't found : " + e);
        } catch (InvalidPathException e) {
            System.err.println("Incorrect input path : " + e);
        }
    }

    /**
     * Produces .class file implementing class or interface specified by provided token.
     * This file is located in the root directory and same package as token.
     *
     * <p>Generated class full name is same as full name of the given token with Impl suffix in the end.
     *
     * @param token token to be implemented
     * @param root  root of the directory where implemented class will be located
     * @throws ImplerException if the given token cannot be generated for one of such reasons:
     *                         <ul>
     *                         <li> Some arguments are null</li>
     *                         <li> Given token is :
     *                             <ol>
     *                             <li>primitive type</li>
     *                             <li>array type</li>
     *                             <li>{@link Enum}</li>
     *                             <li>final class</li>
     *                             <li>private class</li>
     *                             </ol>
     *                         </li>
     *                         <li> token contains only private constructors </li>
     *                         <li> I/O exceptions occurs during implementation </li>
     *                         </ul>
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        assertNotNull("token or root can not be null", token, root);
        checkToken(token);
        Path path = getTokenFilePath(root, token, JAVA);
        createDirectories(path);
        try (var writer = Files.newBufferedWriter(path)) {
            writeAll(writer,
                    implPackage(token),
                    implClassSignature(token),
                    " {" + LINE_SEPARATOR,
                    implConstructors(token),
                    implMethods(token),
                    "}" + LINE_SEPARATOR
            );
        } catch (IOException e) {
            throw new ImplerException("Can not write to the output .class file", e);
        }
    }

    /**
     * Produces .jar file implementing class or interface specified by provided token.
     *
     * <p>Generated class full name is same as full name of the given token with Impl suffix in the end.
     *
     * <p>During implementation creates temporary directory where stores temporary .java and .class files.
     * If method fails to delete temporary directory with files in it, it prints information about occurred problems.
     *
     * @param token   token to be implemented
     * @param jarFile .jar file where the implemented class will be located
     * @throws ImplerException if the given class cannot be created due to one of such reasons:
     *                         <ul>
     *                         <li> Some arguments are null</li>
     *                         <li> The problems with I/O occurred during implementation </li>
     *                         <li> Error occurs during implementation by {@link #implement(Class, Path)} </li>
     *                         <li> {@link JavaCompiler} failed to compile implemented class </li>
     *                         </ul>
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        assertNotNull("token or jarFile can not be null", token, jarFile);
        createDirectories(jarFile);
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(jarFile.getParent(), "tempDir");
        } catch (IOException e) {
            throw new ImplerException("Can not create temp directory : " + e);
        }
        try {
            implement(token, tempDir);

            JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
            assertNotNull("Could not find java compiler", javaCompiler);
            String[] args = {
                    "-cp",
                    tempDir.toString() + File.pathSeparator + getClassPath(token),
                    getTokenFilePath(tempDir, token, JAVA).toString()
            };
            assertEquals("Can not compile impl .class", 0, javaCompiler.run(null, null, null, args));

            try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile))) {
                writer.putNextEntry(new ZipEntry(getTokenFileName(token, "/", CLASS)));
                Files.copy(getTokenFilePath(tempDir, token, CLASS), writer);
            } catch (IOException e) {
                throw new ImplerException("Can not write to jar file : " + e);
            }
        } finally {
            try {
                clean(tempDir);
            } catch (IOException e) {
                System.err.println("Unable to delete temp directory: " + e.getMessage());
            }
        }
    }
}
