Author: Tajung Jang
email: tyj27@drexel.edu
date: 04/28/23
Late days used: 2

instructions:
To run:
    $ sbt run [# of actors]

Then press any key to init the process
    $ ""

To shutdown:
Press any key to end all actors. Then follow up with a "shutdown" message to end the program
    $ ""
    $ shutdown



Analysis:
vector clock:
I don't think it would be that hard to implement vector clocks if you pass them around as a map instead of an actual
vector for book keeping sake. I do this in the current implmenetation anyway when I check whether my request is earlier
than the last received message from all other processes. Updating our local vector would simply be a key, value update.

how many messages are sent for each acquisition: ?
6 messages are sent per acquisition
    1. Request
    2. Ack the request
    3. Acquire lock request to the fake mutex (this is just to simulate actually acquiring a mutex. the mutex actor
    doesn't do anything to actually synchronize things since it doesn't actually reject new lock and release requests
    when its "locked")
    4. Lock request ack from the fake mutex
    5. Release message to fake mutex
    6. Release message to actors

It keeps strong consistency quite efficiently though. None of the messages feel unnessary.
I think its performant enough if you were working in the context of machines all clustered in a LAN but in the context
of globally distributed systems, no because of the latencies involved and lack of fault tolerance.


What would happen if one process simply stopped?
- we would deadlock since there would be no acks coming from the stopped machine