import java.io.File;
import java.io.IOException;
import java.util.*;

class Node {
    int pid;
    int base;
    int size;
    Node prev;
    Node next;

    // Construct used/process segment
    Node(int id, int start, int limit) {
        pid = id;
        base = start;
        size = limit;
        prev = null;
        next = null;
    }
    // Construct free/hole segment
    Node(int start, int limit) {
        pid = -1;
        base = start;
        size = limit;
        prev = null;
        next = null;
    }

    public void setBase(int newbase) { base = newbase; }
    public int getBase() { return base; }
    public void setPid(int newpid) { pid = newpid; }
    public int getPid() { return pid; }
    public void setSize(int newsize) { size = newsize; }
    public int getSize() { return size; }

    @Override
    public String toString() {
        if (pid == -1) {
            return "HOLE [" +
                    "pid=" + pid +
                    ", base=" + base +
                    ", size=" + size +
                    "]";
        }
        return "PROCESS [" +
                "pid=" + pid +
                ", base=" + base +
                ", size=" + size +
                "]";
    }
}

class SortByBase implements Comparator<Node> {
    public int compare(Node a, Node b) {    // sort by ascending order of bases
        return a.getBase() - b.getBase();
    }
}

class SortBySize implements Comparator<Node> {
    public int compare(Node a, Node b) {    // sort by ascending order of sizes
        if (a.getSize() == (b.getSize())) {
            return a.getBase() - b.getBase();
        }
        return a.getSize() - b.getSize();
    }
}

class Memory {
    static int capacity;
    static int remainingSize;
    static Node storage;
    static TreeSet<Node> frees;
    static Map<Integer, Node> pees = new HashMap<>();

    Memory (int cap, TreeSet<Node> treeset) {
        capacity = cap;
        remainingSize = cap;
        frees = treeset;
        Node free = new Node(0, cap);  // create an empty block of free space
        frees.add(free);
        storage = free;
    }

    public void executeFirstFit(Scanner instructions) {
        System.out.println("--- Executing First Fit --- \n");
        while(instructions.hasNextLine()) {
            String cmd = instructions.nextLine();
            String[] cmdArray = breakdownCommand(cmd);
            String func = cmdArray[0];

            if (func.equals("A")) {
                allocateFirstFit(Integer.parseInt(cmdArray[1]), Integer.parseInt(cmdArray[2]));
            } else if (func.equals("D")) {
                deallocate(Integer.parseInt(cmdArray[1]));
            } else {
                printList();
            }
        }
    }

    // first avail block
    static void allocateFirstFit(int id, int size) {
        boolean notFound = true;
        if (size <= remainingSize) {
            for (Node free : frees) {
                if (free.getSize() == size) {  // if perfect fit
                    notFound = false;
                    free.setPid(id);
                    frees.remove(free);
                    pees.put(id, free);
                    remainingSize -= size;
                    break;
                } else if (free.getSize() > size) {
                    notFound = false;
                    Node proc = new Node(id, free.getBase(), size);  //start from base of free
                    if (free.prev != null) { // if free isn't the head, relink
                        proc.next = free;
                        proc.prev = free.prev;
                        proc.prev.next = proc;
                        proc.next.prev = proc;
                    } else {
                        proc.next = free;
                        free.prev = proc;
                        storage = proc;
                    }
                    frees.remove(free);
                    free.setBase(free.getBase() + size);
                    free.setSize(free.getSize() - size);
                    frees.add(free);
                    pees.put(id, proc);
                    remainingSize -= size;
                    break;
                }
            }
            if (notFound) {
                compaction();
                Node free = frees.first();
                Node proc;
                if (free.getSize() == size) {
                    free.setPid(id);
                    proc = free;
                    frees.remove(free);
                } else {
                    proc = new Node(id, free.getBase(), size);
                    proc.next = free;
                    proc.prev = free.prev;
                    proc.prev.next = proc;
                    proc.next.prev = proc;
                    frees.remove(free);
                    free.setBase(free.getBase() + size);
                    free.setSize(free.getSize() - size);
                    frees.add(free);
                }
                pees.put(id, proc);
                remainingSize -= size;
            }
        } else {
            System.out.println("Process PID: " + id + " doesn't fit.  Moving on.\n");
        }
    }

    public void executeBestFit(Scanner instructions) {
        System.out.println("--- Executing Best Fit --- \n");
        while(instructions.hasNextLine()) {
            String cmd = instructions.nextLine();
            String[] cmdArray = breakdownCommand(cmd);
            String func = cmdArray[0];

            if (func.equals("A")) {
                allocateBestFit(Integer.parseInt(cmdArray[1]), Integer.parseInt(cmdArray[2]));
            } else if (func.equals("D")) {
                deallocate(Integer.parseInt(cmdArray[1]));
            } else {
                printList();
            }
        }
    }

    // smallest avail block
    static void allocateBestFit(int id, int size) {
        if (size <= remainingSize) {
            Node proc = new Node(id, 0, size);
            Node free = frees.ceiling(proc);
            if (free == null) {
                compaction();
                free = frees.first();
                if (free.getSize() == size) {
                    free.setPid(id);
                    proc = free;
                    frees.remove(free);
                } else {
                    proc = new Node(id, free.getBase(), size);
                    proc.next = free;
                    proc.prev = free.prev;
                    proc.prev.next = proc;
                    proc.next.prev = proc;
                    frees.remove(free);
                    free.setBase(free.getBase() + size);
                    free.setSize(free.getSize() - size);
                    frees.add(free);
                }
                pees.put(id, proc);
                remainingSize -= size;
            } else {
                if (free.getSize() == size) {  // if perfect fit
                    free.setPid(id);
                    frees.remove(free);
                    pees.put(id, free);
                    remainingSize -= size;
                } else if (free.getSize() > size) {
                    proc.setBase(free.getBase());  //start from base of free
                    if (free.prev != null) { // if free isn't the head, swap
                        proc.next = free;
                        proc.prev = free.prev;
                        proc.prev.next = proc;
                        proc.next.prev = proc;
                    } else {
                        proc.next = free;
                        free.prev = proc;
                        storage = proc;
                    }
                    frees.remove(free);
                    free.setBase(free.getBase() + size);
                    free.setSize(free.getSize() - size);
                    frees.add(free);
                    pees.put(id, proc);
                    remainingSize -= size;
                }
            }
        } else {
            System.out.println("Process PID: " + id + " doesn't fit.  Moving on.\n");
        }
    }

    public void executeWorstFit(Scanner instructions) {
        System.out.println("--- Executing Worst Fit --- \n");
        while(instructions.hasNextLine()) {
            String cmd = instructions.nextLine();
            String[] cmdArray = breakdownCommand(cmd);
            String func = cmdArray[0];

            if (func.equals("A")) {
                allocateWorstFit(Integer.parseInt(cmdArray[1]), Integer.parseInt(cmdArray[2]));
            } else if (func.equals("D")) {
                deallocate(Integer.parseInt(cmdArray[1]));
            } else {
                printList();
            }
        }
    }

    // largest avail block
    static void allocateWorstFit(int id, int size) {
        if (size <= remainingSize) {
            Node free = frees.last();
            if (free.getSize() < size) {  // if first free in sorted not big enough, compact
                compaction();
                free = frees.first();  // get the compacted free node
            }
            if (free.getSize() == size) {  // if perfect fit
                free.setPid(id);
                frees.remove(free);
                pees.put(id, free);
                remainingSize -= size;
            } else if (free.getSize() > size) {
                Node proc = new Node(id, free.getBase(), size);  //start from base of free
                if (free.prev != null) { // if free isn't the head, swap
                    proc.next = free;
                    proc.prev = free.prev;
                    proc.prev.next = proc;
                    proc.next.prev = proc;
                } else {
                    proc.next = free;
                    free.prev = proc;
                    storage = proc;
                }
                frees.remove(free);
                free.setBase(free.getBase() + size);
                free.setSize(free.getSize() - size);
                frees.add(free);
                pees.put(id, proc);
                remainingSize -= size;
            }
        } else {
            System.out.println("Process PID: " + id + " doesn't fit.  Moving on.\n");
        }
    }

    static void printList() {
        System.out.println("===== Current Memory =====");
        Node cur = storage;
        while (cur != null) {
            System.out.println(cur);
            cur = cur.next;
        }
//        System.out.println("\n***Frees***");
//        for(Node block : frees) {
//            System.out.println(block + " prev >> " + block.prev);
//        }
//        System.out.println("\n***Pees***");
//        for(Integer key : pees.keySet()) {
//            System.out.println(key + " : " + pees.get(key) + " PREV>> " + pees.get(key).prev + " NEXT>> " + pees.get(key).next);
//        }
        System.out.println("==========================");
        System.out.println("Remaining capacity: " + remainingSize + "\n");
    }

    static void deallocate(int pid) {
        if (pees.containsKey(pid)) {
            Node p = pees.remove(pid);
            p.setPid(-1);
            frees.add(p);
            remainingSize += p.getSize();
            checkMergeFrees(p);
        }
    }

    static void checkMergeFrees(Node free) {
        if (free.next != null && free.next.getPid() == -1) {
            int rsize = free.next.size;
            frees.remove(free);
            free.setSize(free.getSize() + rsize);
            frees.add(free);
            frees.remove(free.next);
            Node rneigh = free.next.next;
            free.next = rneigh;
            if (rneigh != null) {
                rneigh.prev = free;
            }
        }
        if (free.prev != null && free.prev.getPid() == -1) {
            Node lneigh = free.prev;
            frees.remove(lneigh);
            lneigh.setSize(lneigh.getSize() + free.size);
            frees.add(lneigh);
            frees.remove(free);
            lneigh.next = free.next;
            if (free.next != null) {
                free.next.prev = lneigh;
            }
        }
    }

    static void compaction() {
        System.out.println("--------------> Hold on, Compacting ... \n");
        Node cur = storage;
        int space = 0;
        while(cur.next != null) {
            if (cur.getPid() == -1) {
                if (cur == storage) { // if cur is a free block and head node
                    storage = storage.next;
                    cur.next.prev = null;
                } else {
                    cur.next.prev = cur.prev;
                    cur.prev.next = cur.next;
                }
                space += cur.getSize();
                frees.remove(cur);
            } else {
                cur.setBase(cur.getBase() - space);
            }
            cur = cur.next;
        }
        // expand last hole if exists or else make a new hole and link
        if (cur.getPid() == -1) {
            cur.setBase(cur.getBase() - space);
            cur.setSize(cur.getSize() + space);
        } else {
            Node free = new Node(cur.getBase() + cur.getSize(), space);
            cur.next = free;
            free.prev = cur;
        }
    }

    static String[] breakdownCommand(String command) { return command.split(" "); }
}

public class Lab8 {

    static void processFile(String filename) throws IOException {
        Scanner instructions = new Scanner(new File(filename));
        String algo = instructions.nextLine();
        int capacity = Integer.parseInt(instructions.nextLine());
        System.out.println("Max Capacity: " + capacity);

        if (algo.equals("1")) {
            TreeSet<Node> freeTree = new TreeSet<>(new SortByBase());
            Memory mem = new Memory(capacity, freeTree);
            mem.executeFirstFit(instructions);
        } else if (algo.equals("2")) {
            TreeSet<Node> freeTree = new TreeSet<>(new SortBySize());
            Memory mem = new Memory(capacity, freeTree);
            mem.executeBestFit(instructions);
        } else {
            TreeSet<Node> freeTree = new TreeSet<>(new SortBySize());
            Memory mem = new Memory(capacity, freeTree);
            mem.executeWorstFit(instructions);
        }
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            String filename = args[0];
            try {
                processFile(filename);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("Invalid number of arguments.");
        }
    }
}