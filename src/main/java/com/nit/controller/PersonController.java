package com.nit.controller;

import com.nit.dao.PersonRepository;
import com.nit.entity.Person;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
public class PersonController {

    @Autowired
    private PersonRepository repository;

    //Load this graphqls file in my controller.
    @Value("classpath:person.graphqls")
    private Resource schemaResource;

    //Inject the GraphQL
    private GraphQL graphQL;

    //Parse it graphqls file using pre-defined classes
    @PostConstruct  //I want this Wiring and parsing the schema and building the sceham should be around once my application is Up so we written @PostConstruct
    public void loadSchema() throws IOException {
        //first we get the file
        File schemaFile = schemaResource.getFile();
        //parse the schema using TypeDefinitionRegistry
        TypeDefinitionRegistry registry = new SchemaParser().parse(schemaFile);
        //we need to do RuntimeWiring
        RuntimeWiring wiring = buildWiring();
        //Give this RuntimeWiring to GraphQLSchema..pass the registry and wriring
        GraphQLSchema schema = new SchemaGenerator().makeExecutableSchema(registry, wiring);
        //Build it GraphQL..pass the schema here
        graphQL = GraphQL.newGraphQL(schema).build();
    }

    //we created RuntimeWiring
    private RuntimeWiring buildWiring() {
        //In DataFetcher we will fetch the data from database and Per each method call from typescript file, that we will configure in RuntimeWiring using DataFetcher
        //DataFetcher is a functional interface so writing lambda expression here
        DataFetcher<List<Person>> fetcher1 = data -> {
            return (List<Person>) repository.findAll();
        };

        //will return only the Person Object based on the input email
        DataFetcher<Person> fetcher2 = data -> {
            return repository.findByEmail(data.getArgument("email"));
        };

        //we added above two data fecther and we need to set it in appropriate typescript call
        return RuntimeWiring.newRuntimeWiring()
                .type("Query",
                        typeWriting -> typeWriting.dataFetcher("getAllPerson", fetcher1)
                                                  .dataFetcher("findPerson", fetcher2))
                .build();
    }

    //@RequestBody is coming in JSON format so that Jackson will take care to format our MediaType
    @PostMapping("/addPerson")
    public String addPerson(@RequestBody List<Person> persons) {
        repository.saveAll(persons);
        return "record inserted " + persons.size();
    }

    @GetMapping("/findAllPerson")
    public List<Person> getPersons() {
        return (List<Person>) repository.findAll();
    }

    //Below 2 methods we achieve the GraphQL features based on our request input it will fetch the required field.
    //getAllPerson method from graphqls
    @PostMapping("/getAll")
    public ResponseEntity<Object> getAll(@RequestBody String query) {
        ExecutionResult result = graphQL.execute(query);
        return new ResponseEntity<Object>(result, HttpStatus.OK);
    }

    //findPerson(email: String) : Person
    @PostMapping("/getPersonByEmail")
    public ResponseEntity<Object> getPersonByEmail(@RequestBody String query) {
        ExecutionResult result = graphQL.execute(query);
        return new ResponseEntity<Object>(result, HttpStatus.OK);
    }

}
