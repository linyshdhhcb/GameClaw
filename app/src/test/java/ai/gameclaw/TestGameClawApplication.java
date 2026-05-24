package ai.gameclaw;

import org.springframework.boot.SpringApplication;
import org.testcontainers.utility.TestcontainersConfiguration;

public class TestGameClawApplication {

    public static void main(String[] args) {
        SpringApplication.from(GameClawApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
