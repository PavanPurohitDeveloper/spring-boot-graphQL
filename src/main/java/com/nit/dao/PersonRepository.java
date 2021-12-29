package com.nit.dao;

import com.nit.entity.Person;
import org.springframework.data.repository.CrudRepository;

public interface PersonRepository extends CrudRepository<Person, Integer> {

    Person findByEmail(String email);

}
