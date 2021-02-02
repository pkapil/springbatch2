package com.example.demo;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;


public class PersonReader implements ItemReader<Person> {

    List<Person> lists = new ArrayList<>();

    private Integer nextindex=0;

    @PostConstruct
    public void fill() {
        lists.add(new Person("one", "one"));
        lists.add(new Person("two", "two"));
        lists.add(new Person("three", "three"));
        lists.add(new Person("four", "four"));
    }

    @Override
    public Person read() throws Exception {
        Person person = null;
        if (nextindex < lists.size()) {
            person = lists.get(nextindex);
            nextindex++;
        } else {
            nextindex = 0;
        }
        return person;
    }
}