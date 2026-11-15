package task02;

import support.Bench;
import java.util.*;

public class PlaylistBenchmark {
    static final class Song {
        final int id;
        Song(int id) { this.id = id; }
    }
    static List<Song> list(boolean linked, Song[] songs) {
        List<Song> result = linked ? new LinkedList<>() : new ArrayList<>();
        Collections.addAll(result, songs);
        return result;
    }
    public static void main(String[] args) throws Exception {
        try (Bench b = new Bench("Task02")) {
            for (int n : new int[]{100,10000,100000}) {
                Song[] songs = new Song[n];
                for (int i = 0; i < n; i++) songs[i] = new Song(i);
                Song extra = new Song(-1);
                for (boolean linked : new boolean[]{false,true}) {
                    String name = linked ? "LinkedList" : "ArrayList";
                    b.measure(name+"_append", n, n, () -> {
                        List<Song> l = linked ? new LinkedList<>() : new ArrayList<>();
                        return () -> { for (Song s : songs) l.add(s); return l.size(); };
                    });
                    for (int position : new int[]{0,n/2}) {
                        String op = position == 0 ? "front_pair" : "middle_pair";
                        b.measure(name+"_"+op,n,200,() -> {
                            List<Song> l = list(linked,songs);
                            return () -> {
                                long sum=0;
                                for (int i=0;i<200;i++) {
                                    l.add(position,extra); sum+=l.remove(position).id;
                                }
                                Bench.require(l.size()==n,"List size changed"); return sum;
                            };
                        });
                    }
                    b.measure(name+"_get_middle",n,200,() -> {
                        List<Song> l = list(linked,songs);
                        return () -> {
                            long sum=0;
                            for(int i=0;i<200;i++) sum+=l.get(n/2).id;
                            return sum;
                        };
                    });
                    b.measure(name+"_iterate",n,n,() -> {
                        List<Song> l=list(linked,songs);
                        return () -> {
                            long sum=0; for(Song s:l) sum+=s.id;
                            Bench.require(sum==(long)n*(n-1)/2,"Iteration mismatch"); return sum;
                        };
                    });
                }
                for(boolean linked:new boolean[]{false,true}) {
                    String name=linked?"LinkedList":"ArrayDeque";
                    b.measure(name+"_upnext_cycle",n,10000,() -> {
                        Deque<Song> q=linked?new LinkedList<>():new ArrayDeque<>();
                        Collections.addAll(q,songs);
                        return () -> {
                            long sum=0;
                            for(int i=0;i<10000;i++) {
                                q.addFirst(extra); q.addLast(extra);
                                sum+=q.removeFirst().id; sum+=q.removeFirst().id;
                            }
                            Bench.require(q.size()==n,"Deque size changed"); return sum;
                        };
                    });
                }
            }
        }
        System.out.println("Correctness: playlist sums and deque sizes verified.");
    }
}
