package lamport_tyj27;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.ActorRef;
import java.util.*;

public class ResourceAcquirer extends AbstractBehavior<ResourceMessage> {
    public static Behavior<ResourceMessage> create(int id, ActorRef<MutexMessage> mutex) {
        return Behaviors.setup(context -> {
            return new ResourceAcquirer(context, id, mutex);
        });
    }

    private List<ActorRef<ResourceMessage>> actorRefList;
    // actor id
    private int myId;
    private ClockInt integerClock;
    // <timestamp, Id)
    private List<AbstractMap.SimpleEntry<Integer, Integer>> requestQueue;
    // <id, timestamp>
    private HashMap<Integer, Integer> lastReceivedMessage;
    private ActorRef<MutexMessage> mutex;
    private boolean released;

    private ResourceAcquirer(ActorContext ctxt, int myId, ActorRef<MutexMessage> mutex) {
        super(ctxt);
        this.actorRefList = new ArrayList<ActorRef<ResourceMessage>>();
        this.myId = myId;
        this.integerClock = new ClockInt();
        this.requestQueue = new ArrayList<AbstractMap.SimpleEntry<Integer, Integer>>();
        this.lastReceivedMessage = new HashMap<Integer, Integer>();
        this.mutex = mutex;
        this.released = true;
    }

    @Override
    public Receive<ResourceMessage> createReceive() {
        // This method is only called once for initial setup
        return newReceiveBuilder()
                // We could register multiple onMessage handlers, for each subclass of ProxyMessage, if we wanted to.
                // By using a single handler for the general message type, it makes it easier to switch handling of all message types simultaneously (in a later project)
                .onMessage(ResourceMessage.class, this::dispatch)
                .build();
    }

    public Behavior<ResourceMessage> dispatch(ResourceMessage msg) {
        switch (msg) {
            case ResourceMessage.Request r:
                //getContext().getLog().info(String.format("[ResourceAcquirer] %d got request from %d time: %d", myId, r.senderId(), integerClock.getCurrentTimestamp()));
                // update last recevied message timestamp T_m from sender P_i
                lastReceivedMessage.put(r.senderId(), r.timeStamp());
                // add the request to the request queue
                requestQueue.add(new AbstractMap.SimpleEntry<>(r.timeStamp(), r.senderId()));
                // update our clock
                integerClock.messageReceived(r.timeStamp());
                // send an ack to the sender
                r.sender().tell(new ResourceMessage.Ack(integerClock.getCurrentTimestamp(), getContext().getSelf(), myId));
                // update our clock
                integerClock.increment();
                break;

            case ResourceMessage.Release r:
                //getContext().getLog().info(String.format("[ResourceAcquirer] %d got release message from %d time: %d", myId, r.senderId(), integerClock.getCurrentTimestamp()));
                // update last recevied message timestamp T_m from sender P_i
                lastReceivedMessage.put(r.senderId(), r.timeStamp());
                // update our clock
                integerClock.messageReceived(r.timeStamp());
                // remove all requests from sender
                removeRequestFromQueue(requestQueue, r.senderId());
                // attempt to acquire the lock if we have a request in the queue
                attempAquire();
                // update our clock
                integerClock.increment();
                break;

            case ResourceMessage.Ack a:
                // update our clock
                integerClock.messageReceived(a.timeStamp());
                //getContext().getLog().info(String.format("[ResourceAcquirer] %d got ack from %d time: %d", myId, a.senderId(), integerClock.getCurrentTimestamp()));
                // update last recevied message timestamp T_m from sender P_i
                lastReceivedMessage.put(a.senderId(), a.timeStamp());
                // attempt to acquire the lock if we have a request in the queue
                attempAquire();
                // update our clock
                integerClock.increment();
                break;

            case ResourceMessage.LockRequestAck a:
                getContext().getLog().info(String.format("[ResourceAcquirer] %d OWNED time: %d", myId, integerClock.getCurrentTimestamp()));
                // send a release message to the mutex
                a.sender().tell(new MutexMessage.Release(getContext().getSelf()));
                // update our clock
                integerClock.increment();
                break;

            case ResourceMessage.LockReleaseAck a:
                // update our clock
                integerClock.increment();
                getContext().getLog().info(String.format("[ResourceAcquirer] %d RELEASING time: %d", myId, integerClock.getCurrentTimestamp()));
                // remove our requests from our own queue
                removeRequestFromQueue(requestQueue, myId);
                // set released to true so that we can send new requests to acquire the lock
                released = true;
                // send release message
                for(ActorRef<ResourceMessage> actorRef : actorRefList) {
                    actorRef.tell(new ResourceMessage.Release(integerClock.getCurrentTimestamp(), getContext().getSelf(), myId));
                }
                break;

            case ResourceMessage.InitKickOff i:
                integerClock.increment();
                // acquire lock first
                getContext().getLog().info(String.format("[ResourceAcquirer] %d ACQUIRING time: %d", myId, integerClock.getCurrentTimestamp()));
                mutex.tell(new MutexMessage.Lock(getContext().getSelf()));
                break;

            case ResourceMessage.InitActorRefList i:
                getContext().getLog().info(String.format("[ResourceAcquirer] %d got inited time: %d", myId, integerClock.getCurrentTimestamp()));
                this.actorRefList = i.actorRefList();
                // remove ourself from the list
                removeFromActorList(this.actorRefList, getContext().getSelf());
                break;

            case ResourceMessage.End s:
                getContext().getLog().info("[ResourceAcquirer] shutting down");
                return Behaviors.stopped();

            default:
                return Behaviors.stopped();
        }
        // Keep the same message handling behavior
        return this;
    }

    private void removeFromActorList(List<ActorRef<ResourceMessage>> list, ActorRef<ResourceMessage> actorID)
    {
        Iterator<ActorRef<ResourceMessage>> iterator = list.iterator();
        while(iterator.hasNext()) {
            ActorRef<ResourceMessage> entry = iterator.next();
            if(entry == actorID) {
                iterator.remove();
            }
        }
    }
    private void removeRequestFromQueue(List<AbstractMap.SimpleEntry<Integer, Integer>> queue, int actorID)
    {
        Iterator<AbstractMap.SimpleEntry<Integer, Integer>> iterator = queue.iterator();
        while(iterator.hasNext()) {
            AbstractMap.SimpleEntry<Integer, Integer> entry = iterator.next();
            if(entry.getValue() == actorID) {
                iterator.remove();
            }
        }
    }

    private boolean checkQueueIfRequestExists(List<AbstractMap.SimpleEntry<Integer, Integer>> queue, int actorID)
    {
        Iterator<AbstractMap.SimpleEntry<Integer, Integer>> iterator = queue.iterator();
        while(iterator.hasNext()) {
            AbstractMap.SimpleEntry<Integer, Integer> entry = iterator.next();
            if(entry.getValue() == actorID) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEarliest(List<AbstractMap.SimpleEntry<Integer, Integer>> list, int actorId) {
        Collections.sort(list, Comparator.comparing(AbstractMap.SimpleEntry::getKey));
        int earliestTimestamp = list.get(0).getKey();
        List<AbstractMap.SimpleEntry<Integer, Integer>> tempList = new ArrayList<>();
        for(var entry : list) {
            if(entry.getKey() == earliestTimestamp) {
                tempList.add(entry);
            }
        }
        Collections.sort(tempList, Comparator.comparing(AbstractMap.SimpleEntry::getValue));
        if(tempList.get(0).getValue() == actorId) {
            return true;
        }
        return false;
    }

    private boolean isOldestTime(HashMap<Integer, Integer> timeStampMap, int time) {
        if(timeStampMap.isEmpty()) {
            return false;
        }
        for(HashMap.Entry<Integer, Integer> set : timeStampMap.entrySet()) {
            if(set.getValue() < time) {
                return false;
            }
        }
        return true;
    }

    private void attempAquire() {
        if(checkQueueIfRequestExists(requestQueue, myId)) {
            if(isEarliest(requestQueue, myId)) {
                // check if we have received some message from all others after our request
                if(isOldestTime(lastReceivedMessage, requestQueue.get(0).getKey())) {
                    // set released bool to false so that we don't accidently send another request if we already have on in the queue
                    released = false;
                    getContext().getLog().info(String.format("[ResourceAcquirer] %d ACQUIRING from release time: %d", myId, integerClock.getCurrentTimestamp()));
                    mutex.tell(new MutexMessage.Lock(getContext().getSelf()));
                }
            }
        } else {
            // if no request queued, we request
            //getContext().getLog().info(String.format("[ResourceAcquirer] %d Requesting lock time: %d", myId, integerClock.getCurrentTimestamp()));
            requestQueue.add(new AbstractMap.SimpleEntry<>(integerClock.getCurrentTimestamp(), myId));
            for(ActorRef<ResourceMessage> actorRef : actorRefList) {
                actorRef.tell(new ResourceMessage.Request(integerClock.getCurrentTimestamp(), getContext().getSelf(), myId));
            }
        }
    }
}