package task04;

import java.util.*;
import support.Bench;

public class Inventory {
    public static List<String> codes(int n) {
        List<String> codes=new ArrayList<>();
        for(int i=0;i<n;i++) codes.add(String.format(Locale.ROOT,"M%06d",i));
        Collections.shuffle(codes,new Random(42)); return codes;
    }
    public static String report(Map<String,Integer> map,List<String> order) {
        StringBuilder out=new StringBuilder();
        for(String code:order) out.append(code).append(':').append(map.get(code)).append('\n');
        return out.toString();
    }
    public static String sortedReport(Map<String,Integer> map) {
        List<String> order=new ArrayList<>(map.keySet());
        Collections.sort(order); return report(map,order);
    }
    public static void main(String[] args) throws Exception {
        try(Bench b=new Bench("Task04")) {
            for(int n:new int[]{100,10000,100000}) {
                List<String> insertion=codes(n);
                List<String> sorted=new ArrayList<>(insertion); Collections.sort(sorted);
                String[] keys=sorted.toArray(new String[0]);
                int[] queries=new int[100000];
                Random random=new Random(731);
                for(int i=0;i<queries.length;i++) queries[i]=random.nextInt(n);
                Map<String,Integer> reference=new HashMap<>();
                for(int i=0;i<n;i++) reference.put(insertion.get(i),i);
                String expectedSorted=report(reference,sorted);
                String expectedInsertion=report(reference,insertion);
                for(int type=0;type<3;type++) {
                    Map<String,Integer> map=type==0?new HashMap<>():
                        type==1?new LinkedHashMap<>():new TreeMap<>();
                    map.putAll(reference);
                    // Rebuild to use the actual controlled insertion order.
                    map.clear(); for(String code:insertion) map.put(code,reference.get(code));
                    String name=map.getClass().getSimpleName();
                    Bench.require(map.get("missing")==null,"Missing-code lookup");
                    if(type==1) Bench.require(new ArrayList<>(map.keySet()).equals(insertion),"Insertion order");
                    if(type==2) Bench.require(new ArrayList<>(map.keySet()).equals(sorted),"Code order");
                    final int kind=type;
                    b.measure(name+"_lookup",n,queries.length,() -> () -> {
                        long sum=0;
                        for(int q:queries) sum+=map.get(keys[q]);
                        return sum;
                    });
                    b.measure(name+"_code_report",n,1,() -> () -> {
                        String output=kind==2?report(map,new ArrayList<>(map.keySet())):sortedReport(map);
                        Bench.require(output.equals(expectedSorted),"Sorted report mismatch");
                        return output.hashCode();
                    });
                    b.measure(name+"_insertion_report",n,1,() -> () -> {
                        List<String> order=kind==1?new ArrayList<>(map.keySet()):insertion;
                        String output=report(map,order);
                        Bench.require(output.equals(expectedInsertion),"Insertion report mismatch");
                        return output.hashCode();
                    });
                    System.out.println(name+" n="+n+" both report orders verified.");
                }
                int[] stocks=new int[n];
                for(int i=0;i<n;i++) stocks[i]=reference.get(keys[i]);
                b.measure("SortedArray_lookup",n,queries.length,() -> () -> {
                    long sum=0;
                    for(int q:queries) sum+=stocks[Arrays.binarySearch(keys,keys[q])];
                    return sum;
                });
                Bench.require(Arrays.binarySearch(keys,"missing")<0,"Missing array key");
            }
        }
    }
}
