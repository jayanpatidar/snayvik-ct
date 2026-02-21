package com.snayvik.kpi;

import com.snayvik.kpi.cli.AdminCreateCommandParser;
import com.snayvik.kpi.security.AdminCliService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
@EnableScheduling
public class KpiApplication {

    public static void main(String[] args) {
        if (isAdminCreateCommand(args)) {
            System.exit(runAdminCreateCommand(args));
        }
        SpringApplication.run(KpiApplication.class, args);
    }

    private static boolean isAdminCreateCommand(String[] args) {
        return args.length >= 2 && "admin".equalsIgnoreCase(args[0]) && "create".equalsIgnoreCase(args[1]);
    }

    private static int runAdminCreateCommand(String[] args) {
        AdminCreateCommandParser.ParsedAdminCreateCommand parsedCommand;
        try {
            String[] commandArgs = java.util.Arrays.copyOfRange(args, 2, args.length);
            parsedCommand = AdminCreateCommandParser.parse(commandArgs);
        } catch (IllegalArgumentException exception) {
            System.err.println("Invalid command: " + exception.getMessage());
            printAdminCreateUsage();
            return 2;
        }

        SpringApplication app = new SpringApplication(KpiApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        List<String> runtimeArgs = new ArrayList<>();
        runtimeArgs.add("--spring.task.scheduling.enabled=false");
        runtimeArgs.add("--spring.jpa.hibernate.ddl-auto=none");
        runtimeArgs.addAll(parsedCommand.springArguments());

        try (ConfigurableApplicationContext context =
                app.run(runtimeArgs.toArray(new String[0]))) {
            context.getBean(AdminCliService.class)
                    .createOrUpdateAdmin(parsedCommand.username(), parsedCommand.password());
            System.out.println("Admin user '" + parsedCommand.username() + "' created or updated.");
            return 0;
        } catch (Exception exception) {
            System.err.println("Failed to create admin user: " + exception.getMessage());
            return 1;
        }
    }

    private static void printAdminCreateUsage() {
        System.err.println("Usage: admin create --username <username> --password <password> [spring args]");
    }
}
