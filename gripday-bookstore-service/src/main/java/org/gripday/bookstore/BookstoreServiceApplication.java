package org.gripday.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BookstoreServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreServiceApplication.class, args);
    }
}