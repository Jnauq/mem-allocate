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
    static ArrayList<Node> frees = new ArrayList<>();
    static HashMap<Integer, Node> pees = new HashMap<>();
    static SortByBase baseComp = new SortByBase();
    static SortBySize sizeComp = new SortBySize();

    Memory (int cap) {
        capacity = cap;
        remainingSize = cap;
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
                System.out.println("Deallocating PID: " + cmdArray[1]);
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
            frees.sort(baseComp); // sort array of free blocks by base/index
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
                    if (free.prev != null) { // if free isn't the head, swap
                        proc.next = free;
                        proc.prev = free.prev;
                        proc.next.prev = proc;
                        proc.prev.next = proc;
                    } else {
                        proc.next = free;
                        free.prev = proc;
                        storage = proc;
                    }
                    free.setBase(free.getBase() + size);
                    free.setSize(free.getSize() - size);
                    pees.put(id, proc);
                    remainingSize -= size;
                    break;
                }
            }
            if (notFound) {
                compaction();
                Node free = frees.get(0);
                Node proc;
                if (free.getSize() == size) {
                    free.setPid(id);
                    proc = frees.remove(0);
                } else {
                    proc = new Node(id, free.getBase(), size);
                    proc.next = free;
                    proc.prev = free.prev;
                    proc.next.prev = proc;
                    proc.prev.next = proc;
                    free.setBase(free.getBase() + size);
                    free.setSize(free.getSize() - size);
                }
                pees.put(id, proc);
                remainingSize -= size;
            }
        } else {
            System.out.println("\nProcess PID: " + id + " doesn't fit.  Moving on.\n");
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
                System.out.println("Deallocating PID: " + cmdArray[1]);
                deallocate(Integer.parseInt(cmdArray[1]));
            } else {
                printList();
            }
        }
    }

    // smallest avail block
    static void allocateBestFit(int id, int size) {
        if (size <= remainingSize) {
            frees.sort(sizeComp); // sort array of free blocks by asc size
            System.out.println("SORTED BY SIZE > BASE *** " + frees);
            Node free;
            int bestFit = searchForNode(size);
            System.out.println("best fit index" + bestFit);
            if (bestFit == -1) {
                compaction();
                free = frees.get(0);
                Node proc;
                if (free.getSize() == size) {
                    free.setPid(id);
                    proc = frees.remove(0);
                } else {
                    proc = new Node(id, free.getBase(), size);
                    proc.next = free;
                    proc.prev = free.prev;
                    proc.next.prev = proc;
                    proc.prev.next = proc;
                    free.setBase(free.getBase() + size);
                    free.setSize(free.getSize() - size);
                }
                pees.put(id, proc);
                remainingSize -= size;
            } else {
                free = frees.get(bestFit);
                System.out.println("best fit = " + free);
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
                        proc.next.prev = proc;
                        proc.prev.next = proc;
                    } else {
                        proc.next = free;
                        free.prev = proc;
                        storage = proc;
                    }
                    free.setBase(free.getBase() + size);
                    free.setSize(free.getSize() - size);
                    pees.put(id, proc);
                    remainingSize -= size;
                }
            }
        } else {
            System.out.println("\nProcess PID: " + id + " doesn't fit.  Moving on.\n");
        }
    }

    static int searchForNode(int size) {  // search for best fit
        int left = 0, right = frees.size() -1, mid = 0;
        while (left < right) {
            mid = left + (right - left)/2;
            if (frees.get(mid).getSize() >= size) { //keep cur discard all bigger blocks on right
                right = mid;
            } else { //discard cur and all left smaller blocks
                left = mid + 1;
            }
        }
        return (frees.get(left).getSize() >= size) ? left : -1;
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
                System.out.println("Deallocating PID: " + cmdArray[1]);
                deallocate(Integer.parseInt(cmdArray[1]));
            } else {
                printList();
            }
        }
    }

    // largest avail block
    static void allocateWorstFit(int id, int size) {
        if (size <= remainingSize) {
            frees.sort(sizeComp.reversed()); // sort array of free blocks by dsc size
            Node free = frees.get(0);
            if (free.getSize() < size) {  // if first free in sorted not big enough, compact
                compaction();
                free = frees.get(0);
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
                    proc.next.prev = proc;
                    proc.prev.next = proc;
                } else {
                    proc.next = free;
                    free.prev = proc;
                    storage = proc;
                }
                free.setBase(free.getBase() + size);
                free.setSize(free.getSize() - size);
                pees.put(id, proc);
                remainingSize -= size;
            }
        } else {
            System.out.println("\nProcess PID: " + id + " doesn't fit.  Moving on.\n");
        }
    }

    static void printList() {
        System.out.println("===== Current Memory =====");
        Node cur = storage;
        while (cur != null) {
            System.out.println(cur);
            cur = cur.next;
        }
        System.out.println("==========================");
        System.out.println("Remaining capacity: " + remainingSize + "\n");
    }

    static void deallocate(int pid) {
        Node p = pees.remove(pid);
        p.setPid(-1);
        frees.add(p);
        remainingSize += p.getSize();
        checkMergeFrees(p);
    }

    static void checkMergeFrees(Node free) {
        System.out.println("\nCheck if merging is needed.");
        if (free.next != null && free.next.getPid() == -1) {
            int rsize = free.next.size;
            free.setSize(free.getSize() + rsize);
            System.out.println("Merging => " + free.next + "\n");
            frees.remove(free.next);
            Node rneigh = free.next.next;
            free.next = rneigh;
            if (rneigh != null) {
                rneigh.prev = free;
            }
        }
        if (free.prev != null && free.prev.getPid() == -1) {
            Node lneigh = free.prev;
            lneigh.setSize(lneigh.getSize() + free.size);
            System.out.println("Merging => " + free.next + "\n");
            frees.remove(free);
            lneigh.next = free.next;
            if (free.next != null) {
                free.next.prev = lneigh;
            }
        }
    }

    static void compaction() {
        System.out.println("\n--------------> Hold on, Compacting ... \n");
        Node cur = storage;
        int space = 0;
        if (storage.getPid() == -1) { storage = storage.next; }
        while(cur.next != null) {
            if (cur.getPid() == -1) {
                space += cur.getSize();
                frees.remove(cur);
            } else {
                cur.setBase(cur.getBase() - space);
                cur.prev = null;
            }
            cur = cur.next;
        }
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
        Memory mem = new Memory(capacity);

        if (algo.equals("1")) {
            mem.executeFirstFit(instructions);
        } else if (algo.equals("2")) {
            mem.executeBestFit(instructions);
        } else {
            mem.executeWorstFit(instructions);
        }
    }

    public static void main(String[] args) {
        String filename = args[0];
        try {
            processFile(filename);
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
