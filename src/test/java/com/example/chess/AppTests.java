package com.example.chess;

import com.google.common.base.MoreObjects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("PointlessArithmeticExpression")
@RunWith(SpringRunner.class)
@SpringBootTest
public class AppTests {

    @Test
    public void contextLoads() {
        List<Person> personList = new ArrayList<Person>() {{
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
    }

    private class Person {
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
