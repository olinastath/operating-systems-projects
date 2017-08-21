This program accepts arguments through standard input or a file. 
The input needs to be repeated two times either in the console or the file, with a space before the beginning of the second copy of the input (i.e. 1 1 X 0 1 E 1000 1 1 X 0 1 E 1000).

Navigate to the correct path in which the program files are and execute:

> javac linker.java
> java linker 
-- type the input in separate line
> [contents of input] 

OR 

> java linker < [name of file which contains doubled input, including file extension if there is one]

i.e.
> javac linker.java
> java linker
> 1 1 X 0 1 E 1000 1 1 X 0 1 E 1000

OR 

-- supposed the file INPUT contains the contents "1 1 X 0 1 E 1000 1 1 X 0 1 E 1000"

> javac linker.java
> java linker < INPUT

If the file INPUT does not contain the input data two times then execute:
> cat INPUT INPUT > INPUT-DOUBLE
> javac linker.java
> java linker < INPUT-DOUBLE
 
WARNING: Make sure that the file INPUT ends with a space character so the two copies of the input are separated by a space. Otherwise, the program won't work.