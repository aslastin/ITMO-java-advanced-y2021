package info.kgeorgiy.ja.slastin.student;

import info.kgeorgiy.java.advanced.student.AdvancedQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedQuery {
    private static final Comparator<Student> NAME_COMPARATOR = Comparator
            .comparing(Student::getLastName, Comparator.reverseOrder())
            .thenComparing(Student::getFirstName, Comparator.reverseOrder())
            .thenComparingInt(Student::getId);

    private static String getFullName(final Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private static <T, R, C extends Collection<R>> C map(
            final Collection<T> collection,
            final Function<T, R> function,
            final Collector<R, ?, C> collector
    ) {
        return collection.stream().map(function).collect(collector);
    }

    private static <T, R> List<R> map(final Collection<T> collection, final Function<T, R> function) {
        return map(collection, function, Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return map(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return map(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final List<Student> students) {
        return map(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return map(students, StudentDB::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return map(students, Student::getFirstName, Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> students) {
        return students.stream().max(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    private static <T> List<T> sort(final Stream<T> stream, final Comparator<T> comparator) {
        return stream.sorted(comparator).collect(Collectors.toList());
    }

    private static <T> List<T> sort(final Collection<T> collection, final Comparator<T> comparator) {
        return sort(collection.stream(), comparator);
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return sort(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sort(students, NAME_COMPARATOR);
    }

    private static <T, R> Predicate<T> getPredicate(final Function<T, R> function, final R equalTo) {
        return student -> function.apply(student).equals(equalTo);
    }

    private static <T, R> Stream<T> filterByEqualTo(
            final Collection<T> collection,
            final Function<T, R> function,
            final R equalTo
    ) {
        return collection.stream().filter(getPredicate(function, equalTo));
    }

    private static <T, R> List<T> filterAndSort(
            final Collection<T> collection,
            final Function<T, R> function,
            final R equalTo,
            final Comparator<T> comparator
    ) {
        return sort(filterByEqualTo(collection, function, equalTo), comparator);
    }

    private static <T> List<Student> filterAndSortStudentsByName(
            final Collection<Student> students,
            final Function<Student, T> function,
            final T equalTo
    ) {
        return filterAndSort(students, function, equalTo, NAME_COMPARATOR);
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return filterAndSortStudentsByName(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return filterAndSortStudentsByName(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final GroupName group) {
        return filterAndSortStudentsByName(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final GroupName group) {
        return filterByEqualTo(students, Student::getGroup, group)
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    private static <T, K, V> Stream<Map.Entry<K, V>> getEntryStream(
            final Collection<T> collection,
            final Function<T, K> keyMapper,
            final Supplier<Map<K, V>> mapFactory,
            final Function<Stream<T>, V> function
    ) {
        return collection.stream()
                .collect(Collectors.groupingBy(keyMapper, mapFactory,
                        Collectors.collectingAndThen(Collectors.<T>toList(), list -> function.apply(list.stream()))))
                .entrySet()
                .stream();
    }

    private static List<Group> getGroups(final Collection<Student> students, final Comparator<Student> comparator) {
        return getEntryStream(students, Student::getGroup, TreeMap::new, studentStream -> sort(studentStream, comparator))
                .map(entry -> new Group(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private static <T, K> K getMax(
            final Collection<T> collection,
            final Function<T, K> keyMapper,
            final Function<Stream<T>, Long> function,
            final Comparator<K> keyComparator
    ) {
        return getEntryStream(collection, keyMapper, HashMap::new, function)
                .max(Map.Entry.<K, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey(keyComparator)))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static <T, K, R> K getMaxDistinct(
            final Collection<T> collection,
            final Function<T, K> keyMapper,
            final Function<T, R> mapper,
            final Comparator<K> keyComparator
    ) {
        return getMax(collection, keyMapper, stream -> stream.map(mapper).distinct().count(), keyComparator);
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return getGroups(students, NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return getGroups(students, Student::compareTo);
    }

    @Override
    public GroupName getLargestGroup(final Collection<Student> students) {
        return getMax(students, Student::getGroup, Stream::count, GroupName::compareTo);
    }

    @Override
    public GroupName getLargestGroupFirstName(final Collection<Student> students) {
        return getMaxDistinct(students, Student::getGroup, Student::getFirstName,
                Collections.reverseOrder(GroupName::compareTo));
    }

    @Override
    public String getMostPopularName(final Collection<Student> students) {
        return students.isEmpty() ? "" :
                getMaxDistinct(students, Student::getFirstName, Student::getGroup, Comparator.naturalOrder());
    }

    private static <T, R> List<R> getByIndices(final List<T> list, final int[] indices, final Function<T, R> function) {
        return Arrays.stream(indices)
                .mapToObj(list::get)
                .map(function)
                .collect(Collectors.toList());
    }

    private static <T, R> List<R> applyFunctionAndGetByIndices(
            final Collection<T> collection,
            final int[] indices,
            final Function<T, R> function
    ) {
        return getByIndices(List.copyOf(collection), indices, function);
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> students, final int[] indices) {
        return applyFunctionAndGetByIndices(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> students, final int[] indices) {
        return applyFunctionAndGetByIndices(students, indices, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final Collection<Student> students, final int[] indices) {
        return applyFunctionAndGetByIndices(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final Collection<Student> students, final int[] indices) {
        return applyFunctionAndGetByIndices(students, indices, StudentDB::getFullName);
    }
}
