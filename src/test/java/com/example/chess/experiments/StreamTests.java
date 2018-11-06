package com.example.chess.experiments;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StreamTests {

    private List<Person> personList;

    @Before
    public void init() {
        personList = new ArrayList<Person>() {{
            add(new Person("Victoria", 12, Gender.FEMALE));
            add(new Person("Kate", 31, Gender.FEMALE));
            add(new Person("John", 10, Gender.MALE));
            add(new Person("Elizabeth", 16, Gender.FEMALE));
            add(new Person("Jack", 41, Gender.MALE));
            add(new Person("Tom", 25, Gender.MALE));
            add(new Person("Lucas", 17, Gender.MALE));
            add(new Person("Robert", 18, Gender.MALE));
            add(new Person("Jessica", 32, Gender.FEMALE));
            add(new Person("Sarah", 43, Gender.FEMALE));
            add(new Person("Ted", 23, Gender.MALE));
        }};
    }

    @Test
    public void test1() {
        Map<Gender, List<Person>> map = personList.stream()
                .sorted(Comparator.comparingInt(Person::getAge))
                .collect(Collectors.groupingBy(Person::getGender));

        map.forEach((gender, people) -> {
            System.out.println("Gender: " + gender);
            for (Person person : people) {
                System.out.println("\t" + person);
            }
        });
        System.out.println();

        assert map.size() == 2;
        assert map.get(Gender.FEMALE).size() == 5;
        assert map.get(Gender.MALE).size() == 6;
    }

    @Test
    public void test2() {
        Map<Integer, Long> collect = personList
                .stream()
                .flatMap(person -> getAnniversariesSet(person.age).stream())
//                .sorted()
//                .peek(System.out::println)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        collect.forEach((age, count) -> System.out.println(age + "->" + count));

        assert collect.get(10).equals(11L);
        assert collect.get(40).equals(2L);
    }

    @Test
    public void test3() {
        Map<String, String> collect = Stream.of("a1", "b2", "c3", "a4", "a5", "c7")
                .collect(Collectors.groupingBy((p) -> p.substring(0, 1),
                        Collectors.mapping((p) -> p.substring(1, 2), Collectors.joining(":"))));

        collect.forEach((s, s2) -> System.out.println(s + "->" + s2));

        assert collect.get("a").equals("1:4:5");
        assert collect.get("b").equals("2");
        assert collect.get("c").equals("3:7");
    }

    @Test
    public void test4() {
        Collector<Object, StringBuilder, String> collector = Collector.of(
                StringBuilder::new,                 // метод_инициализации_аккумулятора
                (b, s) -> b.append(s).append(", "), // метод_обработки_каждого_элемента,
                (b1, b2) -> b1.append(b2),          // метод_соединения_двух_аккумуляторов (нужен для параллельных стримов)
                stringBuilder -> stringBuilder.substring(0, stringBuilder.length() - 2) // метод_последней_обработки_аккумулятора
        );

        String joinString = Stream.of("a1", "b2", "c3", "a4", "a5", "c7")
                .parallel()
                .collect(collector);

        System.out.println("joinString = " + joinString);

        assert joinString.equals("a1, b2, c3, a4, a5, c7");
    }

    @Test
    public void test5() {
        ArrayList<Person> sortedPersonList = new ArrayList<>(personList);
        sortedPersonList.sort(Person::compareTo);

        System.out.println("sortedPersonList:");
        int sum1 = IntStream.range(0, sortedPersonList.size())
                .filter(i -> i < sortedPersonList.size() - 3)
                .mapToObj(sortedPersonList::get)
                .peek(System.out::println)
                .mapToInt(Person::getAge)
                .sum();

        System.out.println("\npersonList:");
        int sum2 = personList.stream()
                .sorted()
                .limit(personList.size() - 3)
                .peek(System.out::println)
                .mapToInt(Person::getAge)
                .sum();

        System.out.println("\nsum1 = " + sum1);
        System.out.println("sum2 = " + sum2);

        assert sum1 == 152;
        assert sum2 == 152;
    }

    @Test
    public void test6() {
        Person youngestPerson = personList.stream()
                .reduce((p1, p2) -> p1.getAge() <= p2.getAge() ? p1 : p2).orElse(null);

        System.out.println("youngestPerson = " + youngestPerson);
        assert youngestPerson == personList.get(2);
    }

    private Set<Integer> getAnniversariesSet(int age) {
        Set<Integer> result = new HashSet<>();
        for (int i = 10; i <= age; i += 5) {
            result.add(i);
        }
        return result;
    }

    @Test
    public void test7() {
        Map<Gender, List<String>> groupingByWithValueMapper = personList
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Person::getGender,
                                Collectors.mapping(
                                        Person::getName,
                                        Collectors.toList())));

        groupingByWithValueMapper.forEach((gender, names) -> {
            System.out.println("Gender: " + gender);
            for (String name : names) {
                System.out.println("\t" + name);
            }
        });

        assert groupingByWithValueMapper.get(Gender.MALE).get(0).equals("John");
        assert groupingByWithValueMapper.get(Gender.MALE).get(5).equals("Ted");
        assert groupingByWithValueMapper.get(Gender.FEMALE).get(2).equals("Elizabeth");
    }

    static class Person implements Comparable<Person> {
        private String name;
        private int age;
        private Gender gender;

        public Person(String name, int age, Gender gender) {
            this.name = name;
            this.age = age;
            this.gender = gender;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public Gender getGender() {
            return gender;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("name", name)
                    .add("age", age)
                    .add("gender", gender)
                    .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            Person person = (Person) o;

            return new EqualsBuilder()
                    .append(age, person.age)
                    .append(name, person.name)
                    .append(gender, person.gender)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(name)
                    .append(age)
                    .append(gender)
                    .toHashCode();
        }

        @Override
        public int compareTo(Person other) {
            return Integer.compare(this.getAge(), other.getAge());
        }
    }

    private enum Gender {
        MALE, FEMALE
    }

}
