package com.example.chess;

import com.google.common.base.MoreObjects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("PointlessArithmeticExpression")
@RunWith(SpringRunner.class)
@SpringBootTest
public class AppTests {

    private static List<Person> personList = new ArrayList<Person>() {{
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

    @Test
    public void contextLoads() {
//        streamTest();
        streamTest2();
    }

    private void streamTest2() {
        Map<Integer, Long> collect = personList
                .stream()
                .flatMap(person -> getAnniversariesSet(person.age).stream())
//                .sorted()
//                .peek(System.out::println)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        System.out.println("-------------");
        collect.forEach((age, count) -> System.out.println(age + "->" + count));
    }

    private void streamTest() {
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

        personList.sort(Comparator.comparingInt(Person::getAge));

        int sum = IntStream.range(0, personList.size())
                .filter(i -> i < personList.size() - 3)
                .mapToObj(personList::get)
                .peek(System.out::println)
                .mapToInt(Person::getAge)
                .sum();
        System.out.println("sum = " + sum);

        Person youngestPerson = personList.stream()
                .reduce((p1, p2) -> p1.getAge() <= p2.getAge() ? p1 : p2).orElse(null);

        System.out.println("youngestPerson = " + youngestPerson);


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
    }


    private Set<Integer> getAnniversariesSet(int age) {
        Set<Integer> result = new HashSet<>();

        for (int i = 10; i <= age; i += 5) {
            result.add(i);
        }

        return result;
    }

    private static class Person {
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
    }

    private enum Gender {
        MALE, FEMALE;
    }


}
