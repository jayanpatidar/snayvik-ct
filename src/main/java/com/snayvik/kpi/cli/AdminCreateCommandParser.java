package com.snayvik.kpi.cli;

import java.util.ArrayList;
import java.util.List;

public final class AdminCreateCommandParser {

    private AdminCreateCommandParser() {
    }

    public static ParsedAdminCreateCommand parse(String[] args) {
        String username = null;
        String password = null;
        List<String> springArguments = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--username=")) {
                username = arg.substring("--username=".length());
            } else if (arg.equals("--username")) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for --username");
                }
                username = args[++i];
            } else if (arg.startsWith("--password=")) {
                password = arg.substring("--password=".length());
            } else if (arg.equals("--password")) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for --password");
                }
                password = args[++i];
            } else {
                springArguments.add(arg);
            }
        }

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("--username is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("--password is required");
        }

        return new ParsedAdminCreateCommand(username.trim(), password, springArguments);
    }

    public record ParsedAdminCreateCommand(String username, String password, List<String> springArguments) {
    }
}
