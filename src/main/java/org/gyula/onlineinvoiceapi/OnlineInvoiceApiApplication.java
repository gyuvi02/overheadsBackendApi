package org.gyula.onlineinvoiceapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class OnlineInvoiceApiApplication {

    public static void main(String[] args) {
        ApplicationContext ct = SpringApplication.run(OnlineInvoiceApiApplication.class, args);
        System.out.println("spring.profiles.active: " + ct.getEnvironment().getProperty("spring.profiles.active"));
        System.out.println("frontend.singletonlist: " + ct.getEnvironment().getProperty("frontend.singletonlist"));
    }

}
