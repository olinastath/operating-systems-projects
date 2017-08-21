import java.io.*;
import java.net.URL;
import java.util.*;

class Process {
	int id;
	int arrivalTime;
	int burst;
	int runRemaining;
	int cpuBase;
	int cpuTime;
	int cpuTimeRemaining;
	int ioBase;
	int finishTime;
	int turnTime;
	int ioTime;
	int waitTime;
	String status;
	int quantum;
	int priority;
	

	public Process(int A, int B, int C, int IO){
		this.arrivalTime = A;
		this.cpuBase = B;
		this.cpuTime = C;
		this.cpuTimeRemaining = C;
		this.runRemaining = 0;
		this.ioBase = IO;
		this.status = "unstarted";
		this.quantum = 2;
	}
}

class CompArrival implements Comparator<Process> {
	public int compare(Process p1, Process p2) {
		if(p1.arrivalTime > p2.arrivalTime) {
			return 1;
		} else if(p1.arrivalTime < p2.arrivalTime) {
			return -1;
		} else if (p1.id > p2.id) {
			return 1;
		} else {
			return -1;
		}
	}
}

class CompId implements Comparator<Process> {
	public int compare(Process p1, Process p2) {
		if (p1.id > p2.id) {
			return 1;
		} else { // two processes will never have the same id
			return -1;
		}
	}
}

class CompPriority implements Comparator<Process> {
	public int compare(Process p1, Process p2) {
		if (p1.priority > p2.priority) {
			return 1;
		} else {
			return -1;
		}
	}
}

class CompRun implements Comparator<Process> {
	public int compare(Process p1, Process p2) {
		if(p1.cpuTimeRemaining > p2.cpuTimeRemaining) {
			return 1;
		} else if(p1.cpuTimeRemaining < p2.cpuTimeRemaining) {
			return -1;
		} else if (p1.id > p2.id) {
			return 1;
		} else {
			return -1;
		}
	}
}

public class Scheduling {
	static ArrayList<Process> processes = new ArrayList<Process>();
	static Queue<Process> readyQueue = new LinkedList<Process>();
	static Stack<Process> readyStack = new Stack<Process>();
	static ArrayList<Process> blockedList = new ArrayList<Process>();
	static ArrayList<Process> addReadyMult = new ArrayList<Process>();
	static ArrayList<Process> unstartedList = new ArrayList<Process>();
	static int n, cycle, block, quantum, termNum = 0, rand = 1;
	static Process pro = null, temp = null;
	static CompArrival compArr = new CompArrival();
	static CompRun compRun = new CompRun();
	static CompId compId = new CompId();
	static CompPriority compPri = new CompPriority();
	static boolean verbose = false;
	static String type = " ";
	
	public static void main(String[] args) throws FileNotFoundException {
		String input = args[args.length - 1];
		if (args.length > 1 && !args[args.length - 2].equals("--verbose")) {
			type = args[args.length - 2];
		}
		
		URL url = Scheduling.class.getResource(input);
		File file = new File(url.getPath());
		Scanner in = new Scanner(file);
		n = in.nextInt(); cycle = 0; block = 0; quantum = 2;
		int counter = n, a, b, c, io;
		
		if (args[0].equals("--verbose")) {
			verbose = true;
		}

		while (counter > 0) {
			a = in.nextInt();
			b = in.nextInt();
			c = in.nextInt();
			io = in.nextInt();
			processes.add(new Process(a, b, c, io));
			counter--;
		}

		switch(type) {
		case "FCFS": FCFS(); break;
		case "RR": RR(quantum); break;
		case "LCFS": LCFS(); break;
		case "PSJF": PSJF(); break;
		default:
			System.out.println("Please select a mode (FCFS, RR, LCFS, or PSJF): ");
			in = new Scanner(System.in);
			type = in.next();
			processes.clear();
			main(args);
		}

	}

	private static void FCFS() throws FileNotFoundException {
		printProcesses();
		
		if (verbose) {
			System.out.println("\n\nThis detailed printout gives the state and remaining burst for each process");
			System.out.print("\nBefore cycle " + cycle + ": ");
			for (int i = 0; i < n; i++) {
				System.out.printf("\t%10s %d", processes.get(i).status, processes.get(i).burst);
			}
		}
		cycle++;
		
		for (int i = 0; i < n; i++) {
			if (processes.get(i).arrivalTime == 0) {
				processes.get(i).status = "ready";
				processes.get(i).burst = 0;
				readyQueue.add(processes.get(i));
			} else {
				processes.get(i).status = "unstarted";
				processes.get(i).burst = 0;
				unstartedList.add(processes.get(i));
			}
		}
	
		while (termNum != n) {
			if (!blockedList.isEmpty()) {
				block++;
			}			
			if (verbose) {
				System.out.print("\nBefore cycle " + cycle + ": ");
			}
			for (int i = 0; i < unstartedList.size(); i++) {
				if (unstartedList.get(i).arrivalTime + 1 == cycle) {
					unstartedList.get(i).status = "ready";
					readyQueue.add(unstartedList.get(i));
				}
			}
			
			if (pro == null) {
				pro = readyQueue.poll();
				if (pro != null) {
					pro.status = "running";
					pro.burst = randomOS(pro.cpuBase);
				}
			}
			
			if (pro != null) {
				pro.burst--;
				pro.cpuTimeRemaining--;
			}
			
			if (verbose) {
				for (int i = 0; i < n; i++) {
					if (processes.get(i).status.equals("running")) {
						System.out.printf("\t%10s %d", processes.get(i).status, 1 + processes.get(i).burst);
					} else {
						System.out.printf("\t%10s %d", processes.get(i).status, processes.get(i).burst);
					}
				}
			}
			
			if (!readyQueue.isEmpty()) {
				for (Process p: readyQueue) {
					p.waitTime++;
				}
			}
			
			if (!blockedList.isEmpty()) {
				int add = 0;
				ArrayList<Process> addReady = new ArrayList<Process>();
				Process[] blocked = blockedList.toArray(new Process[0]);
				
				for (int i = 0; i < blocked.length; i++) {
					blocked[i].burst--;
					blocked[i].ioTime++;
					if (blocked[i].burst == 0) {
						add++;
						blocked[i].status = "ready";
						addReady.add(blocked[i]);
						blockedList.remove(blocked[i]);
					}
				}
				
				if (add == 1) {
					readyQueue.add(addReady.get(0));
				} else {
					Collections.sort(addReady, compId);
					readyQueue.addAll(addReady);
				}
			}
			
			if (pro != null) {
				if (pro.cpuTimeRemaining == 0) {
					pro.status = "terminated";
					pro.burst = 0;
					pro.finishTime = cycle;
					pro.turnTime = pro.finishTime - pro.arrivalTime;
					termNum++;
					pro = null;
				} else {
					if (pro.burst <= 0) {
						pro.status = "blocked";
						pro.burst = randomOS(pro.ioBase);
						blockedList.add(pro);
						pro = null;
					}
				}
			}
			
			cycle++;
		}
		System.out.printf("\n\nThe scheduling algorithm used was First Come First Served\n\n");
		printSummary();
	}
	
	private static void RR(int quantum) throws FileNotFoundException {
		printProcesses();
		
		if (verbose) {
			System.out.println("\n\nThis detailed printout gives the state and remaining burst for each process");
			System.out.print("\nBefore cycle " + cycle + ": ");
			for (int i = 0; i < n; i++) {
				System.out.printf("\t%10s %d", processes.get(i).status, processes.get(i).burst);
			}
		}
		cycle++;
		
		for (int i = 0; i < n; i++) {
			if (processes.get(i).arrivalTime == 0) {
				processes.get(i).status = "ready";
				processes.get(i).burst = 0;
				readyQueue.add(processes.get(i));
			} else {
				processes.get(i).status = "unstarted";
				processes.get(i).burst = 0;
				unstartedList.add(processes.get(i));
			}
		}
	
		while (termNum != n) {
			if (!blockedList.isEmpty()) {
				block++;
			}			
			if (verbose) {
				System.out.print("\nBefore cycle " + cycle + ": ");
			}
			for (int i = 0; i < unstartedList.size(); i++) {
				if (unstartedList.get(i).arrivalTime + 1 == cycle) {
					unstartedList.get(i).status = "ready";
					readyQueue.add(unstartedList.get(i));
				}
			}

			if (pro == null) {
				pro = readyQueue.poll();
				if (pro != null) {
					pro.status = "running";
					if (pro.burst == 0) {
						if (pro.runRemaining != 0) {
							pro.burst = pro.runRemaining;
							pro.runRemaining = 0;
						} else {
						pro.burst = randomOS(pro.cpuBase);
						}
					}
				}
			}
			
			if (pro != null) {
				pro.burst--;
				pro.cpuTimeRemaining--;
				pro.quantum--;
				quantum--;
			}
			
			if (verbose) {
				for (int i = 0; i < n; i++) {
					if (processes.get(i).status.equals("running")) {
						if (processes.get(i).burst == 0) {
							System.out.printf("\t%10s %d", processes.get(i).status, 1 + processes.get(i).burst);
						} else {
							System.out.printf("\t%10s %d", processes.get(i).status, 1 + processes.get(i).quantum);
						}
					} else {
						System.out.printf("\t%10s %d", processes.get(i).status, processes.get(i).burst);
					}
				}
			}
			
			if (!readyQueue.isEmpty()) {
				for (Process p: readyQueue) {
					p.waitTime++;
				}
			}
			
			if (!blockedList.isEmpty()) {
				int add = 0;
				ArrayList<Process> addReady = new ArrayList<Process>();
				Process[] blocked = blockedList.toArray(new Process[0]);
				
				for (int i = 0; i < blocked.length; i++) {
					blocked[i].burst--;
					blocked[i].ioTime++;
					if (blocked[i].burst == 0) {
						add++;
						blocked[i].status = "ready";
						addReady.add(blocked[i]);
						blockedList.remove(blocked[i]);
					}
				}
				
				if (add == 1) {
					addReadyMult.add(addReady.get(0));
				} else {
					Collections.sort(addReady, compArr);
					addReadyMult.addAll(addReady);
				}
			}
			
			if (pro != null) {
				if (pro.cpuTimeRemaining == 0) {
					pro.status = "terminated";
					pro.burst = 0;
					pro.finishTime = cycle;
					pro.turnTime = pro.finishTime - pro.arrivalTime;
					pro.quantum = 2;
					termNum++;
					pro = null;
					quantum = 2;
				} else {
					if (pro.burst == 0) {
						pro.status = "blocked";
						pro.burst = randomOS(pro.ioBase);
						pro.quantum = 2;
						blockedList.add(pro);
						pro = null;
						quantum = 2;
					} else {
						if (quantum == 0) {
							pro.status = "ready";
							pro.runRemaining = pro.burst;
							pro.burst = 0;
							pro.quantum = 2;
							addReadyMult.add(pro);
							pro = null;
							quantum = 2;
						}					
					}
				}
			}
			cycle++;
			
			Collections.sort(addReadyMult, compArr);
			for (int i = 0; i < addReadyMult.size(); i++) {
				readyQueue.add(addReadyMult.get(i));
			}
			addReadyMult.clear();
		}
		System.out.printf("\n\nThe scheduling algorithm used was Round Robbin\n\n");
		printSummary();
	}

	private static void LCFS() throws FileNotFoundException {
		printProcesses();
		
		if (verbose) {
			System.out.println("\n\nThis detailed printout gives the state and remaining burst for each process");
			System.out.print("\nBefore cycle " + cycle + ": ");
			for (int i = 0; i < n; i++) {
				System.out.printf("\t%9s %d", processes.get(i).status, processes.get(i).burst);
			}
		}
		cycle++;
		
		for (int i = 0; i < n; i++) {
			if (processes.get(i).arrivalTime == 0) {
				processes.get(i).status = "ready";
				processes.get(i).burst = 0;
				addReadyMult.add(processes.get(i));
			} else {
				processes.get(i).status = "unstarted";
				processes.get(i).burst = 0;
				unstartedList.add(processes.get(i));
			}
		}

		
		while (termNum != n) {
			if (!blockedList.isEmpty()) {
				block++;
			}			
			if (verbose) {
				System.out.print("\nBefore cycle " + cycle + ": ");
			}
			for (int i = 0; i < unstartedList.size(); i++) {
				if (unstartedList.get(i).arrivalTime + 1 == cycle) {
					unstartedList.get(i).status = "ready";
					addReadyMult.add(unstartedList.get(i));
				}
			}
			
			Collections.sort(addReadyMult, compId);
			for (int i = addReadyMult.size() - 1; i >= 0; i--) {
				readyStack.add(addReadyMult.get(i));
			}
			addReadyMult.clear();

			if (pro == null) {
				if (!readyStack.isEmpty()) {
					pro = readyStack.pop();
				}
				if (pro != null) {
					pro.status = "running";
					pro.burst = randomOS(pro.cpuBase);
				}
			}
			
			if (pro != null) {
				pro.burst--;
				pro.cpuTimeRemaining--;
			}
			
			if (verbose) {
				for (int i = 0; i < n; i++) {
					if (processes.get(i).status.equals("running")) {
						System.out.printf("\t%9s %d", processes.get(i).status, 1 + processes.get(i).burst);
					} else {
						System.out.printf("\t%9s %d", processes.get(i).status, processes.get(i).burst);
					}
				}
			}
			
			if (!readyStack.isEmpty()) {
				for (Process p: readyStack) {
					p.waitTime++;
				}
			}
  
			
			if (!blockedList.isEmpty()) {
				int add = 0;
				ArrayList<Process> addReady = new ArrayList<Process>();
				Process[] blocked = blockedList.toArray(new Process[0]);
				
				for (int i = 0; i < blocked.length; i++) {
					blocked[i].burst--;
					blocked[i].ioTime++;
					if (blocked[i].burst == 0) {
						add++;
						blocked[i].status = "ready";
						addReady.add(blocked[i]);
						blockedList.remove(blocked[i]);
					}
				}
				
				if (add == 1) {
					addReadyMult.add(addReady.get(0));
				} else {
					for (Process p:addReady) {
						addReadyMult.add(p);
					}
				}
			}
						
			if (pro != null) {
				if (pro.cpuTimeRemaining == 0) {
					pro.status = "terminated";
					pro.burst = 0;
					pro.finishTime = cycle;
					pro.turnTime = pro.finishTime - pro.arrivalTime;
					termNum++;
					pro = null;
				} else {
					if (pro.burst <= 0) {
						pro.status = "blocked";
						pro.burst = randomOS(pro.ioBase);
						blockedList.add(pro);
						pro = null;
					}
				}
			}
			
			cycle++;
		
		}
		System.out.printf("\n\nThe scheduling algorithm used was Last Come First Served\n\n");
		printSummary();
	}
	
	private static void PSJF() throws FileNotFoundException {
		printProcesses();
		boolean one = false;
		if (verbose) {
			System.out.println("\n\nThis detailed printout gives the state and remaining burst for each process");
			System.out.print("\nBefore cycle " + cycle + ": ");
			for (int i = 0; i < n; i++) {
				System.out.printf("\t%10s %d", processes.get(i).status, processes.get(i).burst);
			}
		}
		cycle++;
		
		for (int i = 0; i < n; i++) {
			if (processes.get(i).arrivalTime == 0) {
				processes.get(i).status = "ready";
				processes.get(i).burst = 0;
				addReadyMult.add(processes.get(i));
			} else {
				processes.get(i).status = "unstarted";
				processes.get(i).burst = 0;
				unstartedList.add(processes.get(i));
			}
		}
	
		while (termNum != n) {
			if (cycle == 1) {
				one = true;
			}
			if (!blockedList.isEmpty()) {
				block++;
			}			

			if (verbose && !one) {
				System.out.print("\nBefore cycle " + (cycle - 1) + ": ");
			}
			for (int i = 0; i < unstartedList.size(); i++) {
				if (unstartedList.get(i).arrivalTime + 1 == cycle) {
					unstartedList.get(i).status = "ready";
					addReadyMult.add(unstartedList.get(i));
				}
			}
			
			if (pro != null && !readyQueue.isEmpty()) {
				if (pro.cpuTimeRemaining > readyQueue.peek().cpuTimeRemaining) {
					if (pro.burst == 0) {
						pro.status = "blocked";
						pro.burst = randomOS(pro.ioBase);
						blockedList.add(pro);
					} else {
						pro.status = "ready";
						addReadyMult.add(pro);
					}
					pro = null;
				}
			}
			
			if (pro == null) {
				pro = readyQueue.poll();
				if (pro != null) {  
					pro.status = "running";
					if (pro.burst == 0) {
						pro.burst = randomOS(pro.cpuBase);
					}
				}
			}
			
			if (!readyQueue.isEmpty()) {
				for (Process p: readyQueue) {
					p.waitTime++;
				}
			}
			
			for (Process p:readyQueue) {
				addReadyMult.add(p);
			}
			readyQueue.clear();
			
			
			if (pro != null) {
				pro.burst--;
				pro.cpuTimeRemaining--;
			}
			
			if (verbose && !one) {
				for (int i = 0; i < n; i++) {
					if (processes.get(i).status.equals("running")) {
							System.out.printf("\t%10s %d", processes.get(i).status, 1 + processes.get(i).burst);
					} else {
						System.out.printf("\t%10s %d", processes.get(i).status, processes.get(i).burst);
					}
				}
			}
			
			if (!blockedList.isEmpty()) {
				int add = 0;
				ArrayList<Process> addReady = new ArrayList<Process>(); 
				Process[] blocked = blockedList.toArray(new Process[0]);
				
				for (int i = 0; i < blocked.length; i++) {
					blocked[i].burst--;
					blocked[i].ioTime++;
					if (blocked[i].burst == 0) {
						add++;
						blocked[i].status = "ready";
						addReady.add(blocked[i]);
						blockedList.remove(blocked[i]);
					}
				}
				
				if (add == 1) {
					addReadyMult.add(addReady.get(0));
				} else {
					Collections.sort(addReady, compRun);
					addReadyMult.addAll(addReady);
				}
			}
			
			if (pro != null) {
				if (pro.cpuTimeRemaining == 0) {
					pro.status = "terminated";
					pro.burst = 0;
					pro.finishTime = cycle - 1;
					pro.turnTime = pro.finishTime - pro.arrivalTime;
					termNum++;
					pro = null;
				} else {
					if (pro.burst == 0) {
						pro.status = "blocked";
						pro.burst = randomOS(pro.ioBase);
						blockedList.add(pro);
						pro = null;
					} else {
						pro.status = "ready";
						addReadyMult.add(pro);
						pro = null;
					}
				}
			}
			one = false;
			cycle++;
			
			Collections.sort(addReadyMult, compRun);
			for (int i = 0; i < addReadyMult.size(); i++) {
				readyQueue.add(addReadyMult.get(i));
			}
			addReadyMult.clear();
		}
		cycle--;
		System.out.printf("\n\nThe scheduling algorithm used was Preemptive Shortest Job First\n\n");
		printSummary();
	}

	private static void printSummary() {
		double totalRun = 0;
		double totalTurnaround = 0;
		double totalWait = 0;
		for (int i = 0; i < n; i++) {
			totalRun += processes.get(i).cpuTime;
			totalTurnaround += processes.get(i).turnTime;
			totalWait += processes.get(i).waitTime;
			System.out.println("Process " + i + ": ");
			System.out.printf("\t(A, B, C, IO) = (%d, %d, %d, %d)", processes.get(i).arrivalTime, processes.get(i).cpuBase, processes.get(i).cpuTime, processes.get(i).ioBase);
			System.out.println();
			System.out.println("\tFinishing time: " + processes.get(i).finishTime);
			System.out.println("\tTurnaround time: " + processes.get(i).turnTime);
			System.out.println("\tI/O time: " + processes.get(i).ioTime);
			System.out.println("\tWaiting time: " + processes.get(i).waitTime);
			System.out.println();
		}

		System.out.println("Summary Data: ");
		System.out.println("\tFinishing time: " + (cycle - 1));
		System.out.printf("\tCPU Utilization: %6f\n", totalRun/(double)(cycle - 1));
		System.out.printf("\tI/O Utilization: %6f\n", (double)block/(double)(cycle - 1));
		System.out.printf("\tThroughput: %6f processes per hundred cycles\n", (double)((100*n)/(double)(cycle - 1)));
		System.out.printf("\tAverage turnaround time: %6f\n", totalTurnaround/n);
		System.out.printf("\tAverage waiting time: %6f\n", totalWait/n);
	}

	private static void printProcesses() {
		System.out.printf("The original input was: %d", n);
		for (int i = 0; i < n; i++) {
			System.out.printf("\t%d %d %d %d ", processes.get(i).arrivalTime, processes.get(i).cpuBase, processes.get(i).cpuTime, processes.get(i).ioBase);
			processes.get(i).id = i;
		}

		Collections.sort(processes, compArr);

		System.out.printf("\nThe (sorted) input is:  %d", n);
		for (int i = 0; i < n; i++) {
			System.out.printf("\t%d %d %d %d ", processes.get(i).arrivalTime, processes.get(i).cpuBase, processes.get(i).cpuTime, processes.get(i).ioBase);
			processes.get(i).id = i;
			processes.get(i).priority = i;
		}
	}
	
	private static int randomOS(int U) throws FileNotFoundException {
		URL url = Scheduling.class.getResource("random-numbers.txt");
		File file = new File(url.getPath());
		Scanner in = new Scanner(file);
		int X = 0;
		for (int i = 0; i < rand; i++) {
			X = in.nextInt();
		}
		rand++;
		return 1 + X%U;
	}
}
