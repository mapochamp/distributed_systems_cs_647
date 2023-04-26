package main;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorSystem;

import java.io.*;


public class LamportMutexImplementation {
    // The only IO we're doing here is console IO, if that fails we can't really recover
    public static void main(String[] args) throws IOException {
        System.out.println("Running Java version");
        var orc = ActorSystem.create(Orchestrator.create(), "java-akka");
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