Author: Tajung Jang
Date: 05/31/23

Instructions:


Part 1:
    - I keep 2 states: `unstableState` and `stableState` that both get recomputed everytime there is a new entry
    in the log by `recomputeState` method. It will recompute state up to either the latest entry or up to the commitIndex
    based on the boolean `isStable` passed.

    - The appropriate state is returned based on the message type

    - If a follower is sent a ClientReadStableRequest, it will redirect the client to the leader

    - if the log is empty, the client will know this but reading the unchanged state of the server's state machine:
    aka the # of tickets the server is initialized with.

Part 2:
    - A client will never ask for more tickets than it reads. The server will check to make sure that the # of tickets
    requested does not exceed the number of unstable and stable tickets left.

    - Simulating our closest server going down and looking for a new server is described in the TimeOut message handler
    in the Client class. If we timed out and it is our first time timing out, we look for a new server. If it is our second
    time timing out then we go back to our original server. If there are more than 2 timeouts, the process repeates
    between finding a new server and going back to the original every other timeout.

Part 3:
Implementing on Raft vs Bayou:
    - Similar to bayou, clients work based on inconsistent reads and eventual consistency. Clients will always be able to
    query a server and get an immediate answer even if it doesn't necessarily reflect a committed state yet. Further,
    clients are allowed to make writes based on their inconsistent read. I think doing dependancy checks for a ticket application
    on Raft is easier than Bayou because you're just checking whether the entry is greater than the stable count of tickets are
    before you apply it. While with Bayou, you would have to attach that to every write request. For anything more than a simple
    monotonic ticket counter, resolving conflicts based on reservation, refunds, etc are would be much easier on Bayou by
    using a conflict resolver. On Raft, you would need to create a whole protocol around each edge case to deal with.

    - Stress Workload:
    Implemenation explanation:
        Clients implemeneted by the StressTestClient class are spawned to make unstable reads and ticket buying requests.
        The Orchestrator spawns the StressTestClients and round robins between a list of them to send an unstable read
        request to their designated "closest server". Sleep for 1 second and repeat. This class is different from the main
        Client class because they do not have the ability to simulate shutting down servers. Additionally, under stress mode,
        normal clients are still spawned but they don't timeout and shutdown servers or make any requests that would
        interrupt the stress test clients.

        My code is setup in such a way that for every unstable request, a buy ticket request is sent based on that unstable
        read. So what happens is that everyone reads one value, usually the same value if its the first iteration, and
        send a buyTicket request based off of that. This will repeat and most reads of each iteration will happen
        fast enough such that not all the buy ticket requests have been replicated and committed. This means that
        ever iteration, the client will continue to read off of stale values and the faster the requests are relative to the
        time it takes to repliate and commit, the more the reads will diverge. For the client who's closest server is the
        leader, their unstable read will diverge most from the committed value since all write requests go to the leader only.
        The current setup is that servers start with 100 tickets and clients will randomly try to buy an amount of tickets
        less than or equal to the result of the unstable read.

    Notes on toggling interactive and stress test mode:
        There are comment blocks on the Client.java file. To run stress mode, do not modify the file. To use interactive mode,
        comment out lines 71 and 72. Remove the comment block from line 81 to 107. This section is the section responsible for
        simulating servers shutting down after some reasonable amount of time.

    Note: lines that start with a `//` are comments that explain whats going on and lines in a `` are the commands to run
    Instructions on how to stress test:
        // start 5 servers and 5 clients
        `run 5 5`
        // wait a second for the servers to init and pick a leader. otherwise the clients will start making requests and just get rejected
        `stress`

    - Limits:
    The theoretical limit between an inconsistent read and consistent one would be the initial number of tickets,
    ie # of tickets - 0 = # of tickets. This is because if a node went down immedietly and came back up after the
    consistent state of tickets reflected that it was sold out, the node that went down would still respond with the initial
    number of tickets until it receives a heartbeat. Other than the case of a server outage, if a client attempts to buy
    all available tickets at once and sends the request straight to the leader, the leader would have an inconsistent state of
    0 tickets left and all other replicas would reflect all tickets are still availble until the write gets replicated.
    For the normal case of clients simply spamming commits, I think, it would simply be
        let N = number of clients
        let ticket_buy_request_n be the number of tickets requested by client n in N
        `inconsistent read result - sum(ticket_buy_request_n)`

    because you have to read before every write, the max difference has to be the sum of all requests being reflected in the
    inconsistent state and the committed state.

    In practice, the latency between machines, disk I/O, and the number of clients will all affect the possible number of
    writes being applied before the next read. The fast the IO, lower the latency, and the lower number of clients, the
    chances of a lot of writes being applied before a read gets propagated to the frontend decreases.