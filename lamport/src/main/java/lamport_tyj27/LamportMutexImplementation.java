package lamport_tyj27;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class LamportMutexImplementation {
    // The only IO we're doing here is console IO, if that fails we can't really recover
    public static void main(String[] args) throws IOException {
        System.out.println("Running Java version");
        int numActors = 0;
        if(args.length != 1) {
            System.out.println("Invalid number of args");
            return;
        } else {
            numActors = Integer.parseInt(args[0]);
        }
        var orc = ActorSystem.create(Orchestrator.create(numActors), "java-akka");
        var done = false;
        var console = new BufferedReader(new InputStreamReader(System.in));
        while (!done) {
            var command = console.readLine();
            orc.tell(command);
            if (command.equals("shutdown")) {
                done = true;
                orc.terminate();
            }
        }
    }
}