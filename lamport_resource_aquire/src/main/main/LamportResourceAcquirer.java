package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LamportResourceAcquirer {
    public static void main(String[] args) throws IOException {
        ActorSystem<ResourceAcquirer.Command> lamport = ActorSystem.create(ResourceAcquirer.create(), "Lamport");
        boolean done = false;
        var console = new BufferedReader(new InputStreamReader(System.in));
        while (!done) {
            var command = console.readLine();
            lamport.tell(command);
            if (command.equals("shutdown")) {
                done = true;
                lamport.terminate();
            }
        }
    }
}
