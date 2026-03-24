package com.kevin.algo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * QuantEdge Java Engine — Spring Boot entry point.
 *
 * Start:  mvn spring-boot:run
 *         OR: java -jar target/algo-backtester-java-1.0.0.jar
 *
 * Listens on port 8080. All business logic (strategies, indicators,
 * BacktestEngine) is unchanged — this class just boots the web layer.
 */
@SpringBootApplication
public class QuantEdgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuantEdgeApplication.class, args);
    }
}
