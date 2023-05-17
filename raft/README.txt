1. We don't actually apply any entries because we're not actually doing any runnable procedure calls to execute for the client.
2. Even though the servers can handle multiple requests at once i.e a list of consequtive RPC's, we
have the clients only send a single entry at a time (a single int to represent the request) for simplicity

Instructions:
To run:
    sbt run [# of servers] [# of clients]
    press any key to start the program

To shutdown:
    enter 'shutdown' into console to stop the program

Bugs:
1. No server shutdown command
2. No client RPC commit confirmation message
3. Sometimes I get errors saying Akka doesn't deliver messages
4. Even number of servers leads to candidates only needing 4 votes to become leader (but there will still only ever be one leader)
