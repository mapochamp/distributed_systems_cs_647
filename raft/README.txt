Author: Tajung Jang
Date: 05/17/23

1. We don't actually apply any entries because we're not actually doing any runnable procedure calls to execute for the client.
2. Even though the servers can handle multiple requests at once i.e a list of consequtive RPC's, we
have the clients only send a single entry at a time (a single int to represent the request) for simplicity

Sources consulted:
I had chaptGPT generate the boiler plate code in the FileArray class for file handling and then build on top  of it
for my needs.

Instructions:
To run:
    sbt run [# of servers] [# of clients]
    press any key to start the program

To kill a random server:
    enter 'kill'

To shutdown:
    enter 'shutdown' into console to stop the program

Bugs:
1. No client RPC commit confirmation message
2. Sometimes I get errors saying Akka doesn't deliver messages
3. Even number of servers leads to candidates only needing 4 votes to become leader (but there will still only ever be one leader)
4. Sometimes after restart of a server, log lengths and terms get messed up and there's an infinite election loop where nobody
actually gets elected. Usually happens when a server is killed during an election but sometimes it happens anyway on server restarts.
5. When there are failures involved during an election, sometimes theres race conditions I haven't quite identified the cause of that
makes stuff gets sent around but I think theres something with next index that causes servers to not sync up properly.
