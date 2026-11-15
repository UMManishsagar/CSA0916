package task07;

import java.util.*;
import task04.Inventory;
import support.Bench;

public class Redesign {
    static final class CachedInventory {
        private final LinkedHashMap<String,Integer> stocks;
        private List<String> sortedKeys;
        long comparisons,sorts;
        CachedInventory(Map<String,Integer> source) { stocks=new LinkedHashMap<>(source); }
        void put(String code,int stock) {
            boolean structural=!stocks.containsKey(code); stocks.put(code,stock);
            if(structural) sortedKeys=null;
        }
        void remove(String code) { if(stocks.remove(code)!=null) sortedKeys=null; }
        String codeReport() {
            if(sortedKeys==null) {
                sortedKeys=new ArrayList<>(stocks.keySet());
                sortedKeys.sort((a,b)->{comparisons++;return a.compareTo(b);}); sorts++;
            }
            return Inventory.report(stocks,sortedKeys);
        }
        String insertionReport() { return Inventory.report(stocks,new ArrayList<>(stocks.keySet())); }
    }
    static String original(Map<String,Integer> stocks,long[] counter) {
        List<String> keys=new ArrayList<>(stocks.keySet());
        keys.sort((a,b)->{counter[0]++;return a.compareTo(b);});
        return Inventory.report(stocks,keys);
    }
    public static void main(String[] args) throws Exception {
        LinkedHashMap<String,Integer> test=new LinkedHashMap<>(); test.put("B",2);test.put("A",1);
        CachedInventory cache=new CachedInventory(test);
        Bench.require(cache.codeReport().equals(Inventory.sortedReport(test)),"Initial report");
        cache.put("A",99);test.put("A",99);
        Bench.require(cache.codeReport().equals(Inventory.sortedReport(test))&&cache.sorts==1,"Stock update stale");
        cache.put("C",3);test.put("C",3);
        Bench.require(cache.codeReport().equals(Inventory.sortedReport(test))&&cache.sorts==2,"Insert invalidation");
        cache.remove("B");test.remove("B");
        Bench.require(cache.codeReport().equals(Inventory.sortedReport(test))&&cache.sorts==3,"Delete invalidation");
        cache.remove("A");test.remove("A");cache.put("A",7);test.put("A",7);
        Bench.require(cache.insertionReport().equals(Inventory.report(test,new ArrayList<>(test.keySet()))),"Reinsert order");
        System.out.println("Correctness: stock update, insert, delete and reinsert preserve both reports.");
        try(Bench b=new Bench("Task07")) {
            for(int n:new int[]{100,10000,100000}) {
                LinkedHashMap<String,Integer> stocks=new LinkedHashMap<>();
                int stock=0;for(String code:Inventory.codes(n))stocks.put(code,stock++);
                String expected=Inventory.sortedReport(stocks);
                final int reports=20;
                b.measure("Original_sort_each_report",n,reports,() -> {
                    long[] count={0};
                    return ()->{long sum=0;for(int i=0;i<reports;i++)sum+=original(stocks,count).hashCode();return sum;};
                });
                b.measure("Redesigned_cold_cache",n,reports,() -> {
                    CachedInventory c=new CachedInventory(stocks);
                    return ()->{long sum=0;for(int i=0;i<reports;i++)sum+=c.codeReport().hashCode();return sum;};
                });
                b.measure("Redesigned_warm_cache",n,reports,() -> {
                    CachedInventory c=new CachedInventory(stocks);c.codeReport();
                    return ()->{long sum=0;for(int i=0;i<reports;i++)sum+=c.codeReport().hashCode();return sum;};
                });
                long[] comparisons={0};CachedInventory c=new CachedInventory(stocks);
                for(int i=0;i<reports;i++) {
                    Bench.require(original(stocks,comparisons).equals(expected),"Original content");
                    Bench.require(c.codeReport().equals(expected),"Cached content");
                }
                Bench.require(c.sorts==1 && comparisons[0]==c.comparisons*reports,"Sort reduction");
                System.out.printf("COUNTS n=%d reports=%d original_sorts=%d redesigned_sorts=%d "
                    +"original_comparisons=%d redesigned_comparisons=%d%n",
                    n,reports,reports,c.sorts,comparisons[0],c.comparisons);
            }
        }
    }
}
