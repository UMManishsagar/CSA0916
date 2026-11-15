package task03;

import java.util.*;
import support.Bench;

final class Student {
    private final String regNo;
    private final String name;
    Student(String regNo,String name) {
        this.regNo=Objects.requireNonNull(regNo); this.name=name;
    }
    String regNo() { return regNo; }
    @Override public boolean equals(Object o) {
        if(this==o) return true;
        if(!(o instanceof Student)) return false;
        return regNo.equals(((Student)o).regNo);
    }
    // Completed TODO: equal students must have equal hashes.
    @Override public int hashCode() { return regNo.hashCode(); }
}

public class Attendance {
    static final class BrokenStudent {
        final String regNo;
        BrokenStudent(String regNo) { this.regNo=regNo; }
        @Override public boolean equals(Object o) {
            return o instanceof BrokenStudent && regNo.equals(((BrokenStudent)o).regNo);
        }
        // Intentionally broken: inherits Object.hashCode().
    }
    static final class MutableStudent {
        String regNo;
        MutableStudent(String r) { regNo=r; }
        @Override public boolean equals(Object o) {
            return o instanceof MutableStudent && regNo.equals(((MutableStudent)o).regNo);
        }
        @Override public int hashCode() { return regNo.hashCode(); }
    }
    public static void main(String[] args) throws Exception {
        Set<BrokenStudent> before=new HashSet<>();
        before.add(new BrokenStudent("R1")); before.add(new BrokenStudent("R1"));
        before.add(new BrokenStudent("R2"));
        System.out.println("Before completing hashCode: Unique students: "+before.size());
        String[][] tests={{"R1","R1","R2"},{"R1","R2","R3"},{"Aa","BB","Aa"}};
        int[] expected={2,3,2};
        try(Bench b=new Bench("Task03")) {
            for(int t=0;t<tests.length;t++) {
                Set<Student> s=new HashSet<>();
                Set<Student> tree=new TreeSet<>(Comparator.comparing(Student::regNo));
                Map<String,Student> map=new HashMap<>();
                for(String id:tests[t]) {
                    Student student=new Student(id,"Name for "+id);
                    s.add(student); tree.add(student); map.putIfAbsent(id,student);
                }
                Bench.require(s.size()==expected[t] && tree.size()==s.size() && map.size()==s.size(),
                    "De-duplication mismatch");
                System.out.printf("Test %d predicted=%d HashSet=%d TreeSet=%d HashMap=%d%n",
                    t+1,expected[t],s.size(),tree.size(),map.size());
            }
            Student a=new Student("R1","A"), c=new Student("R1","Changed name");
            Bench.require(a.equals(c) && c.equals(a) && a.hashCode()==c.hashCode(),"Contract");
            Bench.require(!a.equals(null) && !a.equals("R1"),"Type/null equality");
            Set<Student> same=new HashSet<>(); same.add(a); same.add(a);
            Bench.require(same.size()==1,"Same-reference duplicate");
            Bench.require("Aa".hashCode()=="BB".hashCode(),"Collision fixture");
            MutableStudent m=new MutableStudent("A");
            Set<MutableStudent> mutable=new HashSet<>(); mutable.add(m); m.regNo="B";
            System.out.println("After key mutation: contains="+mutable.contains(m)
                +" remove="+mutable.remove(m)+" size="+mutable.size());
            Set<Student> corrected=new HashSet<>();
            corrected.add(a); corrected.add(c); corrected.add(new Student("R2","B"));
            System.out.println("Unique students: "+corrected.size());
            Bench.require(corrected.size()==2,"Expected output");
        }
        System.out.println("Correctness: three fixtures, collision, identity, null and alternatives verified.");
    }
}
