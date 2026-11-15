package task05;

import java.util.*;
import support.Bench;

class Team {
    final String name; final int points; final int goalDiff;
    Team(String name,int points,int goalDiff) {
        this.name=name; this.points=points; this.goalDiff=goalDiff;
    }
    @Override public String toString() { return name+"("+points+","+goalDiff+")"; }
}

class Leaderboard<T> {
    private final List<T> items=new ArrayList<>();
    private final Comparator<? super T> order;
    Leaderboard(Comparator<? super T> order) { this.order=Objects.requireNonNull(order); }
    void add(T item) { items.add(item); }
    void addAll(List<? extends T> source) { items.addAll(source); }
    List<T> ranking() {
        // Completed TODO: sorted copy leaves insertion history intact.
        List<T> copy=new ArrayList<>(items); copy.sort(order); return copy;
    }
    List<T> insertionOrder() { return new ArrayList<>(items); }
    static <U extends Comparable<? super U>> List<U> natural(List<? extends U> source) {
        List<U> result=new ArrayList<>(source); Collections.sort(result); return result;
    }
}

public class RankingDemo {
    static final Comparator<Team> ORDER=Comparator.comparingInt((Team t)->t.points).reversed()
        .thenComparing(Comparator.comparingInt((Team t)->t.goalDiff).reversed())
        .thenComparing(t->t.name);
    static void contract(List<Team> teams) {
        for(Team a:teams) for(Team b:teams) {
            Bench.require(Integer.signum(ORDER.compare(a,b))==-Integer.signum(ORDER.compare(b,a)),"Symmetry");
            for(Team c:teams) {
                if(ORDER.compare(a,b)<=0 && ORDER.compare(b,c)<=0)
                    Bench.require(ORDER.compare(a,c)<=0,"Transitivity");
                if(ORDER.compare(a,b)==0)
                    Bench.require(Integer.signum(ORDER.compare(a,c))==Integer.signum(ORDER.compare(b,c)),"Equal substitution");
            }
        }
    }
    public static void main(String[] args) throws Exception {
        List<Team> ties=Arrays.asList(new Team("Delta",12,5),new Team("Beta",12,8),
            new Team("Alpha",12,8),new Team("Echo",9,10),new Team("Charlie",12,5));
        Leaderboard<Team> board=new Leaderboard<>(ORDER); board.addAll(ties);
        List<Team> ranking=board.ranking();
        Bench.require(ranking.toString().equals("[Alpha(12,8), Beta(12,8), Charlie(12,5), Delta(12,5), Echo(9,10)]"),"Tie order");
        Bench.require(board.insertionOrder().equals(ties),"Original modified");
        ranking.clear(); Bench.require(board.ranking().size()==5,"Ranking leaks internal list");
        System.out.println("Ranking: "+board.ranking());
        List<Team> adversarial=Arrays.asList(new Team("Low",Integer.MIN_VALUE,0),
            new Team("High",Integer.MAX_VALUE,0),new Team("Mid",0,0),
            new Team("Alpha",1,1),new Team("Alpha",1,1));
        Comparator<Team> faulty=(a,b)->b.points-a.points;
        Bench.require(faulty.compare(adversarial.get(1),adversarial.get(0))>0,"Overflow fixture");
        contract(ties); contract(adversarial);
        System.out.println("Faulty subtraction comparator misorders MIN/MAX; correct comparator verified.");
        List<Team> stable=new ArrayList<>(ties);
        stable.sort(Comparator.comparing(t->t.name));
        stable.sort(Comparator.comparingInt((Team t)->t.goalDiff).reversed());
        stable.sort(Comparator.comparingInt((Team t)->t.points).reversed());
        Bench.require(stable.equals(board.ranking()),"Stable sorts differ");
        Bench.require(Leaderboard.natural(Arrays.asList(3,1,2)).equals(Arrays.asList(1,2,3)),"Generic bound");
        try(Bench b=new Bench("Task05")) {
            for(int n:new int[]{100,10000,100000}) {
                List<Team> data=new ArrayList<>(); Random r=new Random(42);
                for(int i=0;i<n;i++) data.add(new Team("T"+i,r.nextInt(100),r.nextInt(30)));
                b.measure("ComparatorChain_sort",n,1,() -> {
                    List<Team> copy=new ArrayList<>(data);
                    return ()->{ copy.sort(ORDER); return copy.get(0).points+copy.size(); };
                });
                b.measure("ThreeStable_sorts",n,1,() -> {
                    List<Team> copy=new ArrayList<>(data);
                    return ()->{
                        copy.sort(Comparator.comparing(t->t.name));
                        copy.sort(Comparator.comparingInt((Team t)->t.goalDiff).reversed());
                        copy.sort(Comparator.comparingInt((Team t)->t.points).reversed());
                        return copy.get(0).points+copy.size();
                    };
                });
            }
        }
        System.out.println("Correctness: ties, comparator contract, overflow, copy isolation and stable sorts PASS.");
    }
}
