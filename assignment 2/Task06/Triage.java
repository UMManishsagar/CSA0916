package task06;

import java.util.*;
import support.Bench;

class Patient {
    final String name; final int severity; final long arrival;
    Patient(String name,int severity,long arrival) {
        this.name=name; this.severity=severity; this.arrival=arrival;
    }
    @Override public String toString() { return name+"(S"+severity+")"; }
}

public class Triage {
    private long counter=0;
    static final Comparator<Patient> ORDER=Comparator.comparingInt((Patient p)->p.severity)
        .thenComparingLong(p->p.arrival);
    // Completed TODO: lower severity first; first arrival breaks equal-severity ties.
    final PriorityQueue<Patient> queue=new PriorityQueue<>(ORDER);
    void admit(String name,int severity) {
        if(severity<1||severity>5) throw new IllegalArgumentException("Severity must be 1..5");
        queue.add(new Patient(name,severity,counter++));
    }
    Patient next() { return queue.poll(); }
    static List<Patient> fixture() {
        int[] levels={3,1,2,1,5,2,4,3}; List<Patient> result=new ArrayList<>();
        for(int i=0;i<levels.length;i++) result.add(new Patient("P"+(i+1),levels[i],i));
        return result;
    }
    static List<String> pollAll(PriorityQueue<Patient> q) {
        List<String> names=new ArrayList<>(); while(!q.isEmpty()) names.add(q.poll().name);
        return names;
    }
    // A frozen priority snapshot. Rebuild periodically; never use a changing-clock comparator.
    static PriorityQueue<Patient> aged(Collection<Patient> waiting,long now,long interval) {
        if(interval<=0) throw new IllegalArgumentException("Positive aging interval required");
        Comparator<Patient> order=Comparator.comparingInt((Patient p)->
            (int)Math.max(1L,p.severity-Math.max(0L,now-p.arrival)/interval))
            .thenComparingLong(p->p.arrival);
        PriorityQueue<Patient> q=new PriorityQueue<>(order); q.addAll(waiting); return q;
    }
    public static void main(String[] args) throws Exception {
        Triage triage=new Triage();
        for(Patient p:fixture()) triage.admit(p.name,p.severity);
        System.out.println("Heap storage view (not a sorted report): "+triage.queue);
        List<String> actual=pollAll(triage.queue);
        List<String> expected=Arrays.asList("P2","P4","P3","P6","P1","P8","P7","P5");
        Bench.require(actual.equals(expected),"Treatment order");
        Bench.require(triage.next()==null,"Empty queue");
        boolean rejected=false; try {triage.admit("bad",0);} catch(IllegalArgumentException e) {rejected=true;}
        Bench.require(rejected,"Invalid severity");
        System.out.println("Hand prediction: "+expected+"; observed: "+actual);
        LinkedList<Patient> sorted=new LinkedList<>();
        List<Deque<Patient>> buckets=new ArrayList<>();
        for(int i=0;i<5;i++) buckets.add(new ArrayDeque<>());
        for(Patient p:fixture()) {
            ListIterator<Patient> it=sorted.listIterator();
            while(it.hasNext()) { if(ORDER.compare(p,it.next())<0) {it.previous(); break;} }
            it.add(p); buckets.get(p.severity-1).addLast(p);
        }
        List<String> linkedOrder=new ArrayList<>(),bucketOrder=new ArrayList<>();
        while(!sorted.isEmpty()) linkedOrder.add(sorted.removeFirst().name);
        for(Deque<Patient> bucket:buckets) while(!bucket.isEmpty()) bucketOrder.add(bucket.removeFirst().name);
        Bench.require(linkedOrder.equals(expected)&&bucketOrder.equals(expected),"Alternative order");
        List<Patient> waiting=Arrays.asList(new Patient("OldMinor",5,0),new Patient("NewCritical",1,50));
        Bench.require(aged(waiting,0,10).peek().name.equals("NewCritical"),"Initial severity");
        Bench.require(aged(waiting,50,10).peek().name.equals("OldMinor"),"Aging promotion");
        System.out.println("Aging: OldMinor reaches priority 1 and wins arrival tie after waiting.");
        try(Bench b=new Bench("Task06")) {
            for(int n:new int[]{100,10000,100000}) {
                Patient[] patients=new Patient[n]; Random r=new Random(42);
                for(int i=0;i<n;i++) patients[i]=new Patient("P"+i,1+r.nextInt(5),i);
                b.measure("PriorityQueue_admit",n,n,() -> {
                    PriorityQueue<Patient> q=new PriorityQueue<>(ORDER);
                    return ()->{for(Patient p:patients)q.add(p);return q.size();};
                });
                b.measure("PriorityQueue_serve",n,n,() -> {
                    PriorityQueue<Patient> q=new PriorityQueue<>(ORDER); Collections.addAll(q,patients);
                    return ()->{long sum=0;while(!q.isEmpty())sum+=q.poll().arrival;return sum;};
                });
                PriorityQueue<Patient> check=new PriorityQueue<>(ORDER); Collections.addAll(check,patients);
                Patient last=null; while(!check.isEmpty()) {
                    Patient p=check.poll();
                    Bench.require(last==null||ORDER.compare(last,p)<=0,"Growth ordering"); last=p;
                }
            }
        }
        System.out.println("Correctness: hand order, FIFO ties, alternatives, invalid/empty and aging PASS.");
    }
}
