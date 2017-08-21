import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

class linker{

	public static void main(String[] args) { 
		@SuppressWarnings("resource")
		Scanner in = new Scanner(System.in);
		int modules = in.nextInt();
		int def, use, txt, temp, index;
		int reloc = 0;
		String sym, type, warn;
		int loc, addr;

		int[] modSize = new int[modules];

		HashMap<String, symbol> symTable = new HashMap<String, symbol>();

		for (int i = 0; i < modules; i++) {
			def = in.nextInt();
			while (def > 0) {
				sym = in.next();
				loc = in.nextInt() + reloc;
				if (!symTable.containsKey(sym)) {
					symbol s = new symbol(loc, false, " ", i+1);
					symTable.put(sym, s);
				} else {
					symTable.get(sym).warning = "Error: This variable is multiply defined; first value used";
				}
				def--;
			}
			use = in.nextInt();
			while (use > 0) {
				sym = in.next();
				temp = in.nextInt();
				while (temp != -1) {
					temp = in.nextInt();
				}
				use--;
			}

			txt = in.nextInt();
			modSize[i] = txt;
			reloc = reloc + txt;
			while (txt > 0) {
				type = in.next();
				addr = in.nextInt();
				txt--; 
			}
		}

		String[] useMap = new String[reloc];
		String[] warnings = new String[reloc];
		String[] moreWarnings = new String[reloc];
		HashMap<Integer, Integer> memMap = new HashMap<Integer, Integer>();

		// Start of second pass
		reloc = 0;
		modules = in.nextInt();
		index = 0;

		for (int i = 0; i < modules; i++) {
			def = in.nextInt();
			while (def > 0) {
				sym = in.next();
				loc = in.nextInt();
				if (loc >= modSize[i]) {
					loc = 0;
					for (int j = 0; j < symTable.get(sym).module - 1; j ++) {
						loc = loc + modSize[j];
					}
					warn = "Error: Definition exceeds module size; first word in module used.";
					symTable.get(sym).warning = warn;
					symTable.get(sym).loc = loc;
				} else {
					loc = reloc + loc;
					warn = " ";
				}

				def--;
			}
			use = in.nextInt();
			while (use > 0) {
				sym = in.next();
				temp = in.nextInt(); 
				warn = "";


				while (temp != -1) {
					if (temp < modSize[i]) {
						if (symTable.containsKey(sym)) {
							symTable.get(sym).isUsed = true;
						}
						if (useMap[temp + reloc] == null) {
							useMap[temp + reloc] = sym;
							warnings[temp + reloc] = " ";
						} else {
							warnings[temp + reloc] = "Error: Multiple variables used in instruction; all but first ignored.";
						}
						temp = in.nextInt();
					} else {
						temp = in.nextInt();
						moreWarnings[index] = "Error: Use of " + sym + " in module " + (i+1) + " exceeds module size; use ignored.";
					}			
				}
				use--;
			}

			txt = in.nextInt();
			temp = reloc;
			reloc = reloc + txt;
			while (txt > 0) {
				type = in.next();
				addr = in.nextInt();
				if (type.equals("I")) {
					memMap.put(index, addr);
				}
				if (type.equals("A")) { //absolute/immediate: unchanged
					if ((addr%1000) >= 200) {
						addr = (addr/1000) * 1000;
						memMap.put(index, addr);
						warnings[index] = "Error: Absolute address exceeds machine size; zero used.";
					} else {
						memMap.put(index, addr);
					}
					
				} else if (type.equals("E")) { //external: change 
					sym = useMap[index];
					if (sym != null) {
						if (!symTable.containsKey(sym)) {
							addr = (addr/1000) * 1000;
							memMap.put(index, addr);
							warnings[index] = "Error: " + sym + " is not defined; zero used.";
						} else {
							addr = (addr/1000) * 1000 + symTable.get(sym).loc;
							memMap.put(index, addr);
						}
					} 
				} else if (type.equals("R")) { //relative: add relocation constant to the address
					
					if ((addr%1000) >= modSize[i]) {
						addr = (addr/1000) * 1000;
						memMap.put(index, addr);
						warnings[index] = "Error: Relative address exceeds module size; zero used.";
					} else {
						addr = addr + temp;
						memMap.put(index, addr);
					}
				}
				index++;
				txt--;
			}
		}

		System.out.println("Symbol Table:");
		Set<String> keys = symTable.keySet();

		
		for (int i = 0; i < keys.toArray().length; i++) {
			System.out.print(keys.toArray()[i] + " = " + symTable.get(keys.toArray()[i]).loc + " " + symTable.get(keys.toArray()[i]).warning);
			System.out.println();
		}

		for (int i = 0; i < warnings.length; i++) {
			if (warnings[i] == null) {
				warnings[i] = "";
			}
		}

		System.out.println();
		System.out.println("Memory Map:");
		Set<Integer> memIn = memMap.keySet();

		for (int i = 0; i < memIn.size(); i++) {
			System.out.println(i + ":\t" + memMap.get(i) + " " + warnings[i]);
		}

		System.out.println();

		for (int i = 0; i < keys.toArray().length; i++) {
			if (!symTable.get(keys.toArray()[i]).isUsed) {
				System.out.println("Warning: " + keys.toArray()[i] + " was defined in module " + symTable.get(keys.toArray()[i]).module + " but never used.");
			}
			if (i < moreWarnings.length && moreWarnings[i] != null) {
				System.out.println(moreWarnings[i]);
			}
		}
		
	}
}

class symbol {
	int loc;
	boolean isUsed;
	String warning;
	int module;

	public symbol(int loc, boolean isUsed, String warning, int module) {
		this.loc = loc;
		this.isUsed = isUsed;
		this.warning = warning;
		this.module = module;
	}

}
