import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.Scanner;

public class Paging{
	static int M, P, S, J, N, mode, numProcesses, rand = 1;
	static String R;
	static Frame[] frameTable; // stores frames of the machine
	static Process[] processes; // stores processes
	static double[][] probabilities; // stores probability values based on job mix
	static LinkedList<Integer> FIFO = new LinkedList<Integer>(); // keep track of order in which frames where used
	
	public static void main(String[] args) throws FileNotFoundException {
		double A, B, C, y;
		int time, usedFrames = 0; 
		int counter = 1, match = -1, evict = -1;
		boolean fault = true, hit = false;
		
		if (args.length != 7) { 
			System.out.println("Wrong number of inputs.");
		} else {
			// Read standard input and assign values to variable
			M = Integer.parseInt(args[0]); // machine size
			P = Integer.parseInt(args[1]); // page size
			S = Integer.parseInt(args[2]); // process size
			J = Integer.parseInt(args[3]); // job mix
			N = Integer.parseInt(args[4]); // references per process
			R = args[5]; // replacement algorithm 
			mode = Integer.parseInt(args[6]); // debugging mode
		}
		
		System.out.printf("The machine size is %d.\n",M);
		System.out.printf("The page size is %d.\n",P);
		System.out.printf("The process size is %d.\n",S);
		System.out.printf("The job mix number is %d.\n",J);
		System.out.printf("The number of references per process is %d.\n",N);
		System.out.printf("The replacement algorithm is %s.\n",R);
		System.out.printf("The level of debugging output is %d.\n\n",mode);
		

		// Assign number of processes and the probabilities according to job mix
		if (J == 1) {
			numProcesses = 1;
			probabilities = new double[][]{{1,0,0,0}};
		} else if (J == 2){
			numProcesses = 4;
			probabilities = new double[][]{{1,0,0,0}, {1,0,0,0}, {1,0,0,0}, {1,0,0,0}};
		} else if (J == 3) {
			numProcesses = 4;
			probabilities = new double[][]{{0,0,0,0}, {0,0,0,0}, {0,0,0,0}, {0,0,0,0}};
		} else if (J == 4) {
			numProcesses = 4;
			probabilities = new double[][]{{0.75,0.25,0,0}, {0.75, 0, 0.25, 0}, {0.75, 0.125, 0.125, 0}, {0.5, 0.125, 0.125, 0.25}};
		} else {
			System.out.println("Wrong job mix number.");
		}
		
		// Create frame table and populate it with Frame objects
		int numFrames = M/P;
		frameTable = new Frame[numFrames];
		for (int i = 0; i < frameTable.length; i++) {
			frameTable[i] = new Frame(i); 
		}
		
		// Create processes array and populate with Process objects
		processes = new Process[numProcesses];
		for (int i = 0; i < processes.length; i++) {
			processes[i] = new Process(i+1); // initialize with id i+1
			processes[i].word = (111*(i+1))%S; // initialize with word as specified
			processes[i].page = processes[i].word/P;
			processes[i].references = N;
			processes[i].load = new int[S/P]; // array to store the load time of each page of the process
		}
		
		time = N * numProcesses; // total number of references aka time the program needs to finish
		Process current = processes[0]; // start with the first process in the array (process 1)
			
		while (time > 0) {
			// check if there is a hit in the frame table
			for (int i = 0; i < frameTable.length; i++) {
				// we need to check both the process id and the page number
				if (frameTable[i].process == current.id && frameTable[i].page == current.page) {
					match = i; // frame at which there was a hit
					hit = true;
					fault = false;
					break;
				}
			}		
			
			if (mode == 1) {
				System.out.printf("%2d references word %2d (page %d) at time %2d: ", current.id, current.word, current.page, counter);
			}
			
			if (hit) {
				frameTable[match].lru = counter; // update time at which the frame was last accessed
				if (mode == 1) {
					System.out.printf("Hit in frame %d\n", frameTable[match].id);
				}
			} else if (usedFrames < numFrames) { // if there are still free frames
				if (mode == 1) {
					System.out.printf("Fault, using free frame %d\n", frameTable[numFrames-usedFrames-1].id);
				}
				for (int i = frameTable.length - 1; i >= 0; i--) {
					if (frameTable[i].page == -1 && frameTable[i].process == -1) {
						FIFO.add(i); // add frame to FIFO to keep track
						frameTable[i].page = current.page; // update frame to store current page 
						frameTable[i].process = current.id; // and process
						frameTable[i].lru = counter; // update time at which the frame was last accessed
						current.faults++; // increment faults for that process
						current.load[current.page] = counter; // set load time for this page of the process
						usedFrames++; // increment number of used frames (fewer free frames available)
						break; // already found a free frame, no need to keep going
					}
				}
			} else if (fault) {
				current.faults++; // increment faults for that process
				current.load[current.page] = counter; // set load time for this page of the process
				if (R.equals("lru")) {
					evict = findLRU(); // find index of least recently used frame
				} else if (R.equals("fifo")) {
					evict = FIFO.remove(); // find index of first used frame
					FIFO.add(evict);
				} else if (R.equals("random")) {
					evict = randomNum()%numFrames; // get index based on random-numbers
				} else {
					System.out.println("Wrong replacement algorithm.");
				}
				if (mode == 1) {
					System.out.printf("Fault, evicting page %d of %d from frame %d\n", frameTable[evict].page, frameTable[evict].process, frameTable[evict].id);
				}
				processes[frameTable[evict].process-1].evictions++; // increment evictions for that process
				processes[frameTable[evict].process-1].residency += counter - processes[frameTable[evict].process-1].load[frameTable[evict].page]; // add time to process' residency 
				frameTable[evict].lru = counter; // update time at which the frame was last accessed
				frameTable[evict].page = current.page; // update page at frame
				frameTable[evict].process = current.id; // update process at frame
				current.load[current.page] = counter; // set load time for this page of the process
			}
						
			y = randomNum()/(Integer.MAX_VALUE + 1d); // get random quotient y
			A = probabilities[current.id-1][0]; // get probabilities for current process
			B = probabilities[current.id-1][1];
			C = probabilities[current.id-1][2];
			
			if (y < A) { // update next word according to given algorithm
				current.word = (current.word + 1)%S;
			} else if (y < (A+B)) {
				current.word = (current.word - 5 + S)%S;
			} else if (y < (A+B+C)) {
				current.word = (current.word + 4)%S;
			} else {
				current.word = randomNum()%S; // update word randomly if y >= (A+B+C)
			}			
					
			current.page = current.word/P; // update page number according to word
			current.quantum--; // decrement quantum so we know when to stop
			current.references--; // decrement references for that process so we know when to stop
			
			if (current.references == 0 || current.quantum == 0) { // if process completed all references or has run out of quantum
				current.quantum = 3; // reset quantum for next time
				current = processes[current.id%numProcesses]; // select next process
			}
			
			time--; // decrement time so we know when to stop 
			counter++; // increment counter so we know what time it is
			hit = false; // reset
			fault = true; // reset
		}
				
		if (mode != 0) { // ~*~*~formatting~*~*~
			System.out.println(); 
		}

		int totalFaults = 0;
		double totalResidency = 0, totalEvictions = 0;

		for (Process p:processes) { // calculate average residency for every process
			if (p.evictions != 0) {
				totalResidency += p.residency;
				totalFaults += p.faults;
				totalEvictions += p.evictions;
				System.out.printf("Process %d had %d faults and %.15f average residency.\n", p.id, p.faults, p.residency / p.evictions);
			} else {
				System.out.printf("Process %d had %d faults.\n\tWith no evictions, the average residency is undefined.\n", p.id, p.faults);
			}
		}
		System.out.println();
		
		if (totalEvictions != 0) { // calculate total average residency
			System.out.printf("The total number of faults is %d and the overall average residency is %f.\n", totalFaults, (double) (totalResidency/totalEvictions));
		} else {
			System.out.printf("The total number of faults is %d.\n\t WIth no evictions, the overall average residency is undefined.\n", totalFaults);
		}
		
	}
	
	

	/*
	 * This function finds the least recently used frame
	 * and returns its index to the main function.
	 */
	private static int findLRU() {
		int min = Integer.MAX_VALUE, index = frameTable.length;
		for (int i = 0; i < frameTable.length; i++) {
			if (frameTable[i].lru < min) {
				min = frameTable[i].lru;
				index = i;
			}
		}
		return index;
	}
	
	/*
	 * This function reads a file of random numbers
	 * and returns the next number in order to the main function.
	 */
	private static int randomNum() throws FileNotFoundException {
		FileReader file = new FileReader("random-numbers.txt");
		Scanner in = new Scanner(file);	
		int X = 0;
		for (int i = 0; i < rand; i++) {
			X = in.nextInt();
		}
		rand++;
		return X;
	}
}

/**
 * This is a Frame object to represent the frames of the frame table.
 * Each Frame has fields page, process, id, and lru.
 * @author Olina St
 *
 */

class Frame {
	int page; // page currently stored in frame
	int process; // process whose page is currently stored in frame
	int id; // id of the frame
	int lru; // time at which frame was last accessed
	
	/*
	 * Constructor using id number
	 */
	public Frame(int id) { 
		this.id = id;
		this.page = -1;
		this.process = -1;
		this.lru = -1;
	}
}

/**
 * This is a Process object to represent the processes of the program.
 * @author Olina St
 *
 */

class Process {
	int id; // process id
	int quantum; // q = 3
	int word; // word process is referencing
	int page; // page word is in
	int references; // references left for the process
	int faults; // faults that have occurred for the process
	double evictions; // # of times a page of this process has been evicted
	double residency; // keep track of residency sum
	int[] load; // store load time for each of the process' pages
		
	/*
	 * Constructor using id number
	 */
	public Process(int id) {
		this.id = id; 
		this.quantum = 3; // given in instructions
		this.word = -1;
		this.page = -1;
		this.references = -1;
		this.faults = 0; // set to 0 because we'll increment
		this.residency = 0; // set to 0 because we'll increment
		this.evictions = 0; // set to 0 because we'll increment
	}
}