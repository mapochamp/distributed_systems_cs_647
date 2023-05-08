package raft;


import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorSystem;

import java.io.*;


public class RaftDemo {
    // The only IO we're doing here is console IO, if that fails we can't really recover
    public static void main(String[] args) throws IOException {
        System.out.println("Running Java version");
        int numServers = 0;
        int numClients = 0;
        if(args.length != 2) {
            System.out.println("Invalid number of args");
            System.out.println("sbt run [num servers] [num clients]");
            return;
        } else {
            numServers = Integer.parseInt(args[0]);
            numClients = Integer.parseInt(args[1]);
        }
        var orc = ActorSystem.create(Orchestrator.create(numServers, numClients), "java-akka");
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
