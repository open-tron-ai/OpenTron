// Native support for basic model listing, with Python fallback for other subcommands.
package io.opentron.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.opentron.core.Utils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Model {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MODEL_LIST_TYPE = new TypeToken<List<Map<String, Object>>>() {}.getType();

    public static void main(String[] args) {
        try {
            if (args.length == 0 || "list".equals(args[0])) {
                listModels();
                return;
            }
            System.err.println("Unknown model command: " + String.join(" ", args));
            printUsage();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Model command failed: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void listModels() throws IOException, InterruptedException {
        String response = Utils.httpGet("/v1/models");
        List<Map<String, Object>> models = GSON.fromJson(response, MODEL_LIST_TYPE);
        if (models == null || models.isEmpty()) {
            System.out.println("No models available.");
            return;
        }

        System.out.printf("%-24s %s%n", "NAME", "DESCRIPTION");
        System.out.printf("%-24s %s%n", "----", "-----------");
        for (Map<String, Object> model : models) {
            System.out.printf("%-24s %s%n", safeString(model.get("name")), safeString(model.get("description")));
        }
    }

    private static String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static void printUsage() {
        System.out.println("Usage: tron model [list]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  list  List available models");
    }
}


