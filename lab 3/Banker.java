import java.io.*;
import java.util.*;

public class Banker {
	static int taskNum; // number of tasks
	static int resNum; // number of resource types
	static CompId compId = new CompId(); // comparator to compare by task id
	static boolean verbose;

	public static void main(String[] args) throws FileNotFoundException {
		String input = args[args.length-1];
		if (args[0].equals("--verbose")) {
			verbose = true;
		} else {
			verbose = false;
		}
		FileReader file = new FileReader(input);
		Scanner in = new Scanner(file);	
		taskNum = in.nextInt(); resNum = in.nextInt();

		// store total number of recourses of each type
		int[] resourcesF = new int[resNum];
		int[] resourcesB = new int[resNum];
		int temp;
		// initialize resources array with number of total resources by type
		for (int i = 0; i < resNum; i++) {
			temp = in.nextInt();
			resourcesF[i] = temp;
			resourcesB[i] = temp;
		}

		// initialize lists of tasks
		ArrayList<Task> tasksF = new ArrayList<Task>();
		ArrayList<Task> tasksB = new ArrayList<Task>();
		
		for (int i = 0; i < taskNum; i++) {
			tasksF.add(new Task(i+1));
			tasksB.add(new Task(i+1));
		}
		
		Activity actF, actB;
		int id; 
		String tempString;
		// populate the activities list of each task in the tasks list
		// TODO make all acts new
		while (in.hasNext()) {
			actF = new Activity();
			actB = new Activity();
			tempString = in.next();
			actF.type = tempString;
			actB.type = tempString;
			id = in.nextInt();
			temp = in.nextInt();
			actF.delay = temp;
			actF.delayCounter = temp;
			actB.delay = temp;
			actB.delayCounter = temp;
			temp = in.nextInt();
			actF.resource = temp;
			actB.resource = temp;
			temp = in.nextInt();
			actF.number = temp;
			actB.number = temp;
			tasksF.get(id-1).activities.add(actF);
			tasksB.get(id-1).activities.add(actB);
		}

		// table to store allocated resources by task
		// initialize as empty -- before any tasks have been allocated  
		int[][] hasTableF = new int[taskNum][resNum];
		int[][] hasTableB = new int[taskNum][resNum];

		// table to store claimed resources by task
		// initialize claim table with initial claims
		int[][] claimTable = new int[taskNum][resNum];
		for (Task t:tasksB) {
			for (Activity a:t.activities) {
				if (a.type.equals("initiate")) {
					claimTable[t.id - 1][a.resource - 1] = a.number;
				}
			}
		}

		// call FIFO
		FIFO(tasksF, resourcesF, hasTableF);

		// call Banker's
		bankers(tasksB, resourcesB, hasTableB, claimTable);
	}

	public static void FIFO(ArrayList<Task> tasks, int[] resources, int[][] hasTable) {
		ArrayList<Task> blocked = new ArrayList<Task>(); // list of blocked tasks
		ArrayList<Task> terminated = new ArrayList<Task>(); // list of terminated tasks
		ArrayList<Task> aborted = new ArrayList<Task>(); // list of aborted tasks
		int[] availRes = resources; // store number of available resources of each type	
									// at the beginning of FIFO all resources are available
		// initialize list for tasks that will be moved from blocked to tasks
		ArrayList<Task> addBack = new ArrayList<Task>();
		boolean granted = false, abort = true, done = false;
		// array of released resources at the end of each cycle
		int[] released = new int[resNum];
		int cycle = 0;

		while (!done) {
			Collections.sort(tasks, compId); // sort tasks by id
			if (verbose) {
				System.out.printf("During %d-%d ", cycle, cycle + 1);
				System.out.print("((");
				for (int i = 0; i < availRes.length; i++) {
					if (i != availRes.length - 1) {
						System.out.print(availRes[i] + ", ");
					} else {
						System.out.print(availRes[i] + ") ");
					}
				}
				System.out.print("units available)\n");
			}
			// check blocked tasks first
			if (!blocked.isEmpty()) {
				// update waiting time
				for (int i = 0; i < blocked.size(); i++) {
					blocked.get(i).waitTime++;
				}

				if (verbose) {
					System.out.println("\tFirst check blocked requests:");
				}
				for (int i = 0; i < blocked.size(); i++) {
					if (blocked.get(i).iterator < blocked.get(i).activities.size()) {
						// check if the activity has a delay
						if (blocked.get(i).activities.get(blocked.get(i).iterator).delayCounter > 0) {
							blocked.get(i).activities.get(blocked.get(i).iterator).delayCounter--;
							if (verbose) {
								System.out.printf("Task %d computes (%d of %d cycles)", blocked.get(i).id, blocked.get(i).activities.get(blocked.get(i).iterator).delay - blocked.get(i).activities.get(blocked.get(i).iterator).delayCounter, blocked.get(i).activities.get(blocked.get(i).iterator).delay);
							}
						} else {
							String type = blocked.get(i).activities.get(blocked.get(i).iterator).type;
							if (type.equals("request")){
								// if the task requests less than are available, grant request
								if (blocked.get(i).activities.get(blocked.get(i).iterator).number <= availRes[blocked.get(i).activities.get(blocked.get(i).iterator).resource - 1]) {
									granted = true;
								}
								if (granted) {
									if (verbose) {
										System.out.printf("\t\tTask %d completes its request\n", blocked.get(i).id);
									}
									// update available resource and allocated resource tables
									availRes[blocked.get(i).activities.get(blocked.get(i).iterator).resource - 1] -= blocked.get(i).activities.get(blocked.get(i).iterator).number;
									hasTable[blocked.get(i).id-1][blocked.get(i).activities.get(blocked.get(i).iterator).resource - 1] += blocked.get(i).activities.get(blocked.get(i).iterator).number;
									blocked.get(i).iterator++;
									// remove task from blocked and add to addBack list
									addBack.add(blocked.remove(i));
									i--; // decrement index because blocked list size has also been decremented
								} else {
									if (verbose) {
										System.out.printf("\t\tTask %d's request still cannot be granted\n", blocked.get(i).id);
									}
									// update request table for each blocked task 
									blocked.get(i).blockReq[0] = blocked.get(i).activities.get(blocked.get(i).iterator).resource;
									blocked.get(i).blockReq[1] = blocked.get(i).activities.get(blocked.get(i).iterator).number;
								}
								granted = false; // reset
							}
						}
					}
				}
			}

			// check the rest of the tasks
			for (int i = 0; i < tasks.size(); i++) {
				if (tasks.get(i).iterator < tasks.get(i).activities.size()) {
					// check if the activity has a delay
					if (tasks.get(i).activities.get(tasks.get(i).iterator).delayCounter > 0) {
						tasks.get(i).activities.get(tasks.get(i).iterator).delayCounter--;
						if (verbose) {
							System.out.printf("\tTask %d computes (%d of %d cycles)\n", tasks.get(i).id, tasks.get(i).activities.get(tasks.get(i).iterator).delay - tasks.get(i).activities.get(tasks.get(i).iterator).delayCounter, tasks.get(i).activities.get(tasks.get(i).iterator).delay);
						}
					} else {
						String type = tasks.get(i).activities.get(tasks.get(i).iterator).type;
						if (type.equals("initiate")) {
							if (verbose) {
								System.out.printf("\tTask %d completed initiate\n", tasks.get(i).id);
							}
							tasks.get(i).iterator++;
						} else if (type.equals("request")){
							// if the task requests less than are available, grant request
							if (tasks.get(i).activities.get(tasks.get(i).iterator).number <= availRes[tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1]) {
								granted = true;
								// update available resource and allocated resource tables
								availRes[tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1] -= tasks.get(i).activities.get(tasks.get(i).iterator).number;
								hasTable[tasks.get(i).id - 1][tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1] += tasks.get(i).activities.get(tasks.get(i).iterator).number;
							}
							if (granted) {
								if (verbose) {
									System.out.printf("\tTask %d completes its request\n", tasks.get(i).id);
								}
								tasks.get(i).iterator++;
							} else {
								if (verbose) {
									System.out.printf("\tTask %d's request cannot be granted\n", tasks.get(i).id);
								}
								// update request table for each blocked task
								tasks.get(i).blockReq[0] = tasks.get(i).activities.get(tasks.get(i).iterator).resource;
								tasks.get(i).blockReq[1] = tasks.get(i).activities.get(tasks.get(i).iterator).number;
								blocked.add(tasks.get(i));	// add task to blocked
								tasks.remove(i);	// remove task from task list
								i--;	// decrement index because task list size has also been decremented
							}
							granted = false;
						} else if (type.equals("release")) {
							if (verbose) {
								System.out.printf("\tTask %d releases %d unit(s)", tasks.get(i).id, tasks.get(i).activities.get(tasks.get(i).iterator).number);
							}
							// update released resource and allocated resource tables
							released[tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1] = tasks.get(i).activities.get(tasks.get(i).iterator).number;
							hasTable[tasks.get(i).id - 1][tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1] -= tasks.get(i).activities.get(tasks.get(i).iterator).number;
							tasks.get(i).iterator++;
							// check if task will terminate
							if (tasks.get(i).activities.get(tasks.get(i).iterator).type.equals("terminate")) {
								if (tasks.get(i).activities.get(tasks.get(i).iterator).delayCounter > 0) {
									tasks.get(i).activities.get(tasks.get(i).iterator).delayCounter--;
									if (verbose) {
										System.out.println();
									}
								} else {
									if (verbose) {
										System.out.printf(" and terminates at %d\n", cycle + 1);
									}
									tasks.get(i).finishTime = cycle + 1;
									terminated.add(tasks.remove(i));
									i--;	// decrement index because task list size has also been decremented
								}
							} else {
								if (verbose) {
									System.out.println();	// ~*~formatting~*~
								}
							}
						} else if (type.equals("terminate")) {
							if (verbose) {
								System.out.printf("\tTask %d terminates at %d\n", tasks.get(i).id, cycle + 1);
							}
							tasks.get(i).finishTime = cycle + 1;
							terminated.add(tasks.remove(i));
							i--;	// decrement index because task list size has also been decremented
						}
					}
				}
			}

			// add tasks back into task list from blocked
			Task temp;
			for (int i = 0; i < addBack.size(); i++) {
				temp = addBack.remove(i);
				tasks.add(temp);
				i--;	// decrement index because list size has also been decremented
			}

			// update available resource table with released resources
			for (int i = 0; i < released.length; i++) {
				availRes[i] += released[i];
				released[i] = 0;	// reset released array
			}

			int resId;
			int resVal;
			// check for deadlock
			if (tasks.isEmpty() && !blocked.isEmpty()) {  
				for (Task b:blocked) {
					resId = b.blockReq[0] - 1;
					resVal = b.blockReq[1];
					if (availRes[resId] >= resVal) {
						abort = false;
					}
				}
				if (abort) {
					if (verbose) {
						System.out.println();
					}
					abort(blocked, aborted, availRes, hasTable);
				}
				abort = true;
			} else if (blocked.isEmpty() && tasks.isEmpty()) {
				// all tasks have terminated or been aborted
				done = true;
			}
			cycle++;
			if (verbose) {
				System.out.println();
			}


		}

		printFIFO(tasks, terminated, aborted, resources);
	}

	public static void abort(ArrayList<Task> blocked, ArrayList<Task> aborted, int[] availRes, int[][] hasTable) {
		boolean resourcesReleased = false;
		Task min; int resId, resVal;
		while (!resourcesReleased && !blocked.isEmpty()) {
			// find the task in blocked list with smallest id
			min = new Task(taskNum + 1);
			for (Task t:blocked) {
				if (t.id <= min.id) {
					min = t;
				}
			}
			blocked.remove(min);	// abort min task
			aborted.add(min);	// add to aborted list
			if (verbose) {
				System.out.printf("Task %d is aborted\n", min.id);
			}
			min.aborted = true;
			// release min's resources
			for (int i = 0; i < resNum; i++) {
				availRes[i] += hasTable[min.id-1][i];
				hasTable[min.id-1][i] = 0;
			}

			for (Task b:blocked) {
				resId = b.blockReq[0] - 1;
				resVal = b.blockReq[1];
				if (availRes[resId] >= resVal) {
					resourcesReleased = true;
				}
			}
		}
	}

	public static void printFIFO(ArrayList<Task> tasks, ArrayList<Task> terminated, ArrayList<Task> aborted, int[] resources) {
		int totalTime = 0, totalWait = 0;
		int totalPercentage; double temp;
		for(Task t:terminated) {
			tasks.add(t);
		}
		for(Task t:aborted) {
			tasks.add(t);
		}
		Collections.sort(tasks, compId);
		System.out.printf("%15s\n", "FIFO");
		for(Task t:tasks) {
			t.percentage = (double) t.waitTime*100/(double) t.finishTime;
			if (t.percentage%1 >= 0.5) {
				t.percentage = (int) Math.ceil(t.percentage);
			} else {
				t.percentage = (int) Math.floor(t.percentage);
			}

			System.out.printf("%s %d\t", "Task", t.id);
			if (t.aborted) {
				System.out.printf(" aborted\n");
			} else {
				System.out.printf("%2d %s %2d \t %2d%%\n", t.finishTime, "", t.waitTime, (int) t.percentage);
				totalWait += t.waitTime;
				totalTime += t.finishTime;
			}			
		}

		temp = (double) totalWait*100/ (double) totalTime;
		if (temp%1 >= 0.5) {
			totalPercentage = (int) Math.ceil(temp);
		} else {
			totalPercentage = (int) Math.floor(temp);
		}
		System.out.printf("%6s \t", "total");
		System.out.printf("%2d %s %2d \t %2d%%\n\n", totalTime, "", totalWait, totalPercentage);
	}

	public static void bankers(ArrayList<Task> tasks, int[] resources, int[][] hasTable, int[][] claimTable) {		
		ArrayList<Task> blocked = new ArrayList<Task>(); // list of blocked tasks
		ArrayList<Task> terminated = new ArrayList<Task>(); // list of terminated tasks
		ArrayList<Task> aborted = new ArrayList<Task>(); // list of aborted tasks
		int[] availRes = resources; // at the beginning of FIFO all resources are available
		// initialize list for tasks that will be moved from blocked to tasks
		ArrayList<Task> addBack = new ArrayList<Task>();
		boolean granted = false, abort = true, done = false;
		// array of released resources at the end of each cycle
		int[] released = new int[resNum];
		int cycle = 0;

		while (!done) {
			Collections.sort(tasks, compId); // sort tasks by id
			if (verbose) {
				System.out.printf("During %d-%d ", cycle, cycle + 1);
				System.out.print("((");
				for (int i = 0; i < availRes.length; i++) {
					if (i != availRes.length - 1) {
						System.out.print(availRes[i] + ", ");
					} else {
						System.out.print(availRes[i] + ") ");
					}
				}
				System.out.print("units available)\n");
			}
			// check blocked tasks first
			if (!blocked.isEmpty()) {
				// update waiting time
				for (int i = 0; i < blocked.size(); i++) {
					blocked.get(i).waitTime++;
				}

				if (verbose) {
					System.out.println("\tFirst check blocked requests:");
				}
				for (int i = 0; i < blocked.size(); i++) {
					if (blocked.get(i).iterator < blocked.get(i).activities.size()) {
						// check if the activity has a delay
						if (blocked.get(i).activities.get(blocked.get(i).iterator).delayCounter > 0) {
							blocked.get(i).activities.get(blocked.get(i).iterator).delayCounter--;
							if (verbose) {
								System.out.printf("Task %d computes (%d of %d cycles)", blocked.get(i).id, blocked.get(i).activities.get(blocked.get(i).iterator).delay - blocked.get(i).activities.get(blocked.get(i).iterator).delayCounter, blocked.get(i).activities.get(blocked.get(i).iterator).delay);
							}
						} else {
							String type = blocked.get(i).activities.get(blocked.get(i).iterator).type;
							if (type.equals("request")){
								// check if the task's  claim is larger than the resources available
								if (blocked.get(i).activities.get(blocked.get(i).iterator).number + hasTable[blocked.get(i).id - 1][blocked.get(i).activities.get(blocked.get(i).iterator).resource - 1]> claimTable[blocked.get(i).id - 1][blocked.get(i).activities.get(blocked.get(i).iterator).resource - 1]) {
									if (verbose) {
										System.out.printf("\tTask %d's request exceeds its claim; aborted.\n", blocked.get(i).id);
									}
									// abort task and release resources
									released[blocked.get(i).activities.get(blocked.get(i).iterator).resource - 1] += blocked.get(i).activities.get(blocked.get(i).iterator).number;
									hasTable[blocked.get(i).id - 1][blocked.get(i).activities.get(blocked.get(i).iterator).resource - 1] -= blocked.get(i).activities.get(blocked.get(i).iterator).number;
									blocked.get(i).aborted = true;
									blocked.get(i).cycleAborted = cycle;
									aborted.add(blocked.remove(i));
									i--;

								} else {
									// if the task requests less than or all the available resources, check if granting the request is safe
									if (blocked.get(i).activities.get(blocked.get(i).iterator).number <= availRes[blocked.get(i).activities.get(blocked.get(i).iterator).resource - 1]) {
										granted = safe(blocked.get(i).id, availRes, claimTable, hasTable);
									}
									if (granted) {
										if (verbose) {
											System.out.printf("\t\tTask %d completes its request\n", blocked.get(i).id);
										}
										// update available resource and allocated resource tables
										availRes[blocked.get(i).activities.get(blocked.get(i).iterator).resource - 1] -= blocked.get(i).activities.get(blocked.get(i).iterator).number;
										hasTable[blocked.get(i).id-1][blocked.get(i).activities.get(blocked.get(i).iterator).resource - 1] += blocked.get(i).activities.get(blocked.get(i).iterator).number;
										blocked.get(i).iterator++;
										// remove task from blocked and add to addBack list
										addBack.add(blocked.remove(i));
										i--; // decrement index because blocked list size has also been decremented
									} else {
										if (verbose) {
											System.out.printf("\t\tTask %d's request still cannot be granted\n", blocked.get(i).id);
										}
										// update request table for each blocked task 
										blocked.get(i).blockReq[0] = blocked.get(i).activities.get(blocked.get(i).iterator).resource;
										blocked.get(i).blockReq[1] = blocked.get(i).activities.get(blocked.get(i).iterator).number;
									}
								}
								granted = false; // reset
							}
						}
					}
				}
			}

			// check the rest of the tasks
			for (int i = 0; i < tasks.size(); i++) {
				if (tasks.get(i).iterator < tasks.get(i).activities.size()) {
					// check if the activity has a delay
					if (tasks.get(i).activities.get(tasks.get(i).iterator).delayCounter > 0) {
						tasks.get(i).activities.get(tasks.get(i).iterator).delayCounter--;
						if (verbose) {
							System.out.printf("\tTask %d computes (%d of %d cycles)\n", tasks.get(i).id, tasks.get(i).activities.get(tasks.get(i).iterator).delay - tasks.get(i).activities.get(tasks.get(i).iterator).delayCounter, tasks.get(i).activities.get(tasks.get(i).iterator).delay);
						}
					} else {
						String type = tasks.get(i).activities.get(tasks.get(i).iterator).type;
						if (type.equals("initiate")) {
							if (tasks.get(i).activities.get(tasks.get(i).iterator).number <= resources[tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1]) {
								if (verbose) {
									System.out.printf("\tTask %d completed initiate\n", tasks.get(i).id);
								}
								tasks.get(i).iterator++;
							} else {
								// if the initial claim is larger than the available resources, abort the task
								if (verbose) {
									System.out.printf("\tTask %d is aborted\n", tasks.get(i).id);
								}
								tasks.get(i).aborted = true;
								tasks.get(i).cycleAborted = cycle;
								aborted.add(tasks.remove(i));
								i--;
							}
						} else if (type.equals("request")){
							// check if the claim is larger than the available resources
							if (tasks.get(i).activities.get(tasks.get(i).iterator).number + hasTable[tasks.get(i).id - 1][tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1]> claimTable[tasks.get(i).id - 1][tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1]) {
								if (verbose) {
									System.out.printf("\tTask %d's request exceeds its claim; aborted.\n", tasks.get(i).id);
								}
								// release any resources and abort task
								released[tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1] += tasks.get(i).activities.get(tasks.get(i).iterator).number;
								hasTable[tasks.get(i).id - 1][tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1] -= tasks.get(i).activities.get(tasks.get(i).iterator).number;
								tasks.get(i).aborted = true;
								tasks.get(i).cycleAborted = cycle;
								aborted.add(tasks.remove(i));
								i--;
								
							} else {
								// if the request is smaller/equal to the available resources, check if granting is safe
								if (tasks.get(i).activities.get(tasks.get(i).iterator).number <= availRes[tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1]) {
									granted = safe(tasks.get(i).id, availRes, claimTable, hasTable);
								}
								
								if (granted) {
									if (verbose) {
										System.out.printf("\tTask %d completes its request\n", tasks.get(i).id);
									}
									// update available resource and allocated resource tables
									availRes[tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1] -= tasks.get(i).activities.get(tasks.get(i).iterator).number;
									hasTable[tasks.get(i).id - 1][tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1] += tasks.get(i).activities.get(tasks.get(i).iterator).number;
									tasks.get(i).iterator++;
								} else {
									if (verbose) {
										System.out.printf("\tTask %d's request cannot be granted (state would not be safe)\n", tasks.get(i).id);
									}
									// update request table for each blocked task
									tasks.get(i).blockReq[0] = tasks.get(i).activities.get(tasks.get(i).iterator).resource;
									tasks.get(i).blockReq[1] = tasks.get(i).activities.get(tasks.get(i).iterator).number;
									blocked.add(tasks.get(i));	// add task to blocked
									tasks.remove(i);	// remove task from task list
									i--;	// decrement index because task list size has also been decremented
								}
							}
							granted = false;						
						} else if (type.equals("release")) {
							if (verbose) {
								System.out.printf("\tTask %d releases %d unit(s)", tasks.get(i).id, tasks.get(i).activities.get(tasks.get(i).iterator).number);
							}
							// update released resource and allocated resource tables
							released[tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1] = tasks.get(i).activities.get(tasks.get(i).iterator).number;
							hasTable[tasks.get(i).id - 1][tasks.get(i).activities.get(tasks.get(i).iterator).resource - 1] -= tasks.get(i).activities.get(tasks.get(i).iterator).number;
							tasks.get(i).iterator++;
							// check if task will terminate
							if (tasks.get(i).activities.get(tasks.get(i).iterator).type.equals("terminate")) {
								if (tasks.get(i).activities.get(tasks.get(i).iterator).delayCounter > 0) {
									tasks.get(i).activities.get(tasks.get(i).iterator).delayCounter--;
									if (verbose) {
										System.out.println();
									}
								} else {
									if (verbose) {
										System.out.printf(" and terminates at %d\n", cycle + 1);
									}
									tasks.get(i).finishTime = cycle + 1;
									terminated.add(tasks.remove(i));
									i--;	// decrement index because task list size has also been decremented
								}
							} else {
								if (verbose) {
									System.out.println();	// ~*~formatting~*~
								}
							}
						} else if (type.equals("terminate")) {
							if (verbose) {
								System.out.printf("\tTask %d terminates at %d\n", tasks.get(i).id, cycle + 1);
							}
							tasks.get(i).finishTime = cycle + 1;
							terminated.add(tasks.remove(i));
							i--;	// decrement index because task list size has also been decremented
						}
					}
				}
			}

			// add tasks back into task list from blocked
			Task temp;
			for (int i = 0; i < addBack.size(); i++) {
				temp = addBack.remove(i);
				tasks.add(temp);
				i--;	// decrement index because list size has also been decremented
			}

			// update available resource table with released resources
			for (int i = 0; i < released.length; i++) {
				availRes[i] += released[i];
				released[i] = 0;	// reset released array
			}

			int resId;
			int resVal;
			// check for deadlock
			if (tasks.isEmpty() && !blocked.isEmpty()) {  
				for (Task b:blocked) {
					resId = b.blockReq[0] - 1;
					resVal = b.blockReq[1];
					if (availRes[resId] >= resVal) {
						abort = false;
					}
				}
				if (abort) {
					if (verbose) {
						System.out.println();
					}
					abort(blocked, aborted, availRes, hasTable);
				}
				abort = true;
			} else if (blocked.isEmpty() && tasks.isEmpty()) {
				// all tasks have terminated or been aborted
				done = true;
			}
			cycle++;
			if (verbose) {
				System.out.println();
			}
		}
		
		printBankers(tasks, terminated, aborted, resources, claimTable);
	}
	
	public static boolean safe(int index, int[] availRes, int[][] claimTable, int[][] hasTable) {
		boolean safe = true;
		// check that there are enough resources available for the given task
		for (int i = 0; i < availRes.length; i++) {
			if (availRes[i] < claimTable[index-1][i] - hasTable[index-1][i]) {
				safe = false;
			}
		}
		return safe;
	}
	
	public static void printBankers(ArrayList<Task> tasks, ArrayList<Task> terminated, ArrayList<Task> aborted, int[] resources, int[][] claimTable) {
		int totalTime = 0, totalWait = 0;
		int totalPercentage; double temp;
		for(Task t:terminated) {
			tasks.add(t);
		}

		boolean before = false;
		for(int i = 0; i < aborted.size(); i++) {
			if (aborted.get(i).cycleAborted == 0) {
				before = true;
			}
		}

		if (before) {
			System.out.print("Banker aborts task(s) ");
			for(int i = 0; i < aborted.size(); i++) {
				if (aborted.get(i).cycleAborted == 0) {
					if (i != aborted.size() - 1) {
						System.out.print(aborted.get(i).id + ", ");
					} else {
						System.out.print(aborted.get(i).id);
					}
				}
			}
			System.out.print(" before run begins.\n");
		}
		for(int i = 0; i < aborted.size(); i++) {
			if (aborted.get(i).cycleAborted == 0) {
				System.out.printf("Task %d: claim for resource %d (%d) exceeds number of units present (%d); aborted.\n", aborted.get(i).id, aborted.get(i).activities.get(0).resource, aborted.get(i).activities.get(0).number, resources[aborted.get(i).activities.get(0).resource - 1]);
			} else {
				System.out.printf("Task %d: request for resource %d (%d) exceeds its claim (%d); aborted.\n", aborted.get(i).id, aborted.get(i).activities.get(0).resource, aborted.get(i).activities.get(0).number, claimTable[aborted.get(i).id-1][aborted.get(i).activities.get(0).resource - 1]);
			}
		}
		
		for(Task t:aborted) {
			tasks.add(t);
		}
		
		Collections.sort(tasks, compId);
		System.out.printf("\n%17s\n", "BANKER'S");
		for(Task t:tasks) {
			t.percentage = (double) t.waitTime*100/(double) t.finishTime;
			if (t.percentage%1 >= 0.5) {
				t.percentage = (int) Math.ceil(t.percentage);
			} else {
				t.percentage = (int) Math.floor(t.percentage);
			}

			System.out.printf("%s %d\t", "Task", t.id);
			if (t.aborted) {
				System.out.printf(" aborted\n");
			} else {
				System.out.printf("%2d %s %2d \t %2d%%\n", t.finishTime, "", t.waitTime, (int) t.percentage);
				totalWait += t.waitTime;
				totalTime += t.finishTime;
			}			
		}

		temp = (double) totalWait*100/ (double) totalTime;
		if (temp%1 >= 0.5) {
			totalPercentage = (int) Math.ceil(temp);
		} else {
			totalPercentage = (int) Math.floor(temp);
		}
		System.out.printf("%6s \t", "total");
		System.out.printf("%2d %s %2d \t %2d%%", totalTime, "", totalWait, totalPercentage);
		System.out.println();
	}
}

class Activity {
	String type;	// store type of activity i.e. initiate, request, release, terminate
	int delay;	// store delay for that activity
	int delayCounter;
	int resource;	// store resource type if requesting/releasing/claiming (0 if terminating)
	int number;	// store number of resources task is requesting/releasing/claiming (0 if terminating)
	
	public Activity() {
		this.type = null;
		this.delay = -1;
		this.delayCounter = -1;
		this.resource = -1;
		this.number = -1;
	}
	
}

class Task {	
	ArrayList<Activity> activities;	// store all the activities for task 
	int iterator;	// keep track of what activity to do next
	int id; // store task id
	int claim; // store initial claims
	int finishTime; // store finishing time
	int waitTime; // store wait time
	double percentage; // store percentage of time spent waiting
	boolean aborted; // keep track of status
	int cycleAborted; // keep track of cycle at which task was aborted
	int[] blockReq; // request table for when a task gets blocked
					// contains resource type at index 0 and amount requested at index 1
	 
	public Task(int id) {
		this.activities = new ArrayList<Activity>();
		this.iterator = 0;
		this.id = id;
		this.claim = -1;
		this.finishTime = -1;
		this.waitTime = 0;
		this.aborted = false;
		this.cycleAborted = -1;
		this.blockReq = new int[2];
	}
}

class CompId implements Comparator<Task> {
	// this comparator compares tasks based on their id
	public int compare(Task t1, Task t2) {
		if (t1.id > t2.id) {
			return 1;
		} else { // two tasks will never have the same id
			return -1;
		}
	}
}