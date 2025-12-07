package com.cargopro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionCheck implements CommandLineRunner {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${DB_HOST:NOT_SET}")
    private String dbHost;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=========================================");
        System.out.println("DATABASE CONFIGURATION CHECK");
        System.out.println("=========================================");
        // Mask the password if present in the URL (though usually it's not in the URL
        // for Postgres if using properties)
        String safeUrl = datasourceUrl.replaceAll("password=.*", "password=*****");
        System.out.println("Effective Datasource URL: " + safeUrl);
        System.out.println("DB_HOST Environment Variable: " + dbHost);
        System.out.println("=========================================");
    }
}
