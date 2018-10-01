import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

// TODO: JAR file can only be run on Windows. Makefile can only be compiled on Linux

public class Makefile
{
	// Compiler executable name
	static final String compilerFlag = "g++";

	// Generate variables
	static String root;
	static String fileName;
	static File makefile;
	static PrintWriter makefileWriter;
	static List<String> cFiles = new ArrayList<String>();
	static List<String> oFiles = new ArrayList<String>();

	static String outputFileName = "";
	static boolean debug = false;

	public static void main(String[] args) throws IOException
	{
		// Gets the root folder
		root = System.getProperty("user.dir");

		if (args.length != 0) {
			for (int i = 0; i < args.length; i++) {

				if (args[i].equals("-o")) {
					outputFileName = args[++i];
				}

				if (args[i].equals("-g"))
					debug = true;
			}
		}

		// Starts the search process
		findCFiles(root);
	}

	/**
	 * Steps through directory that is passed and all its sub directories to
	 * find .cpp files
	 * 
	 * @param root
	 *            Path to search
	 * @throws IOException
	 */
	public static void findCFiles(String root) throws IOException
	{
		// Walks through the directory finding .cpp files
		Files.walk(Paths.get(root)).forEach(filePath ->
			{
				// Checks if current item is a file and has the extension .cpp
				if (Files.isRegularFile(filePath) && filePath.toString().endsWith(".cpp"))
				{
					// Adds .cpp file to an ArrayList for later use
					cFiles.add(filePath.toString());

					System.out.println(filePath);
				}
			});

		// Starts writing the makefile
		writeMakefile(root);
	}

	/**
	 * Writes the makefile according to the root directory
	 * 
	 * @param directory
	 *            Directory to write the makefile in
	 * @throws IOException
	 */
	public static void writeMakefile(String directory) throws IOException
	{
		// Figure out what CXX flags need to be
		String CXX = "";
		if (debug)
			CXX = CXX + "-g ";


		// Turns the cpp file to an o file then adds to oFile ArrayList
		for (String cFile : cFiles)
		{
			cFile = cFile.replace(".cpp", ".o");
			oFiles.add(cFile);
		}

		// Creates the new makefile
		makefile = new File(directory + "\\Makefile");
		makefileWriter = new PrintWriter(makefile);
		makefileWriter.write("# Makefile generated by Makefile Generator (written by Dallan Healey) at " + new Date() + " #\n");

		// Starts the process, calling the main build target all
		makefileWriter.write("all : ");

		if (!outputFileName.equals("")) {
			System.out.println("Output file: " + outputFileName);
			makefileWriter.write(outputFileName);
			makefileWriter.write("\n\n" + outputFileName + " : ");
		}

		// Writes all the o files to the main build target
		for (String oFile : oFiles)
		{
			makefileWriter.write(oFile.substring(oFile.lastIndexOf(directory) + directory.length() + 1) + " ");
		}

		// Compiles the .o files to the binary
		makefileWriter.write("\n\t " + compilerFlag + " ");
		if (!outputFileName.equals("")) {
			makefileWriter.write("-o " + outputFileName + " ");
		}

		for (String oFile : oFiles)
		{
			makefileWriter.write(oFile.substring(oFile.lastIndexOf(directory) + directory.length() + 1) + " ");
		}

		makefileWriter.write("\n");

		// Builds the Object files for the CPP files and any dependencies that the
		// files may have
		for (int i = 0; i < cFiles.size(); i++)
		{
			List<String> depend = readCFile(cFiles.get(i));

			makefileWriter.write((oFiles.get(i).substring(oFiles.get(i).lastIndexOf("\\") + 1)) + " : " + cFiles.get(i).substring(cFiles.get(i).lastIndexOf(directory) + directory.length() + 1) + " ");

			depend.forEach(d ->
				{
					// Checks for the dependency starting with /. If so remove it.
					if (d.startsWith("\\"))
						d = d.substring(1);
					makefileWriter.write(d + " ");
				});

			makefileWriter.write("\n\t " + compilerFlag + " " + CXX +" -c " + cFiles.get(i).substring(cFiles.get(i).lastIndexOf(directory) + directory.length() + 1) + " ");

			makefileWriter.write("\n");
		}

		// TODO: rm doesn't work apparently.
		makefileWriter.write("clean : rm -f *.o " + outputFileName);

		// Closes makefile
		makefileWriter.close();
	}

	/**
	 * Reads the .cpp file and returns a list of dependicies
	 * 
	 * @param fileToRead
	 *            Absolute path to a file that needs to be read for dependicies
	 * @return returns a list of dependicies, or '#include <>/"", in the .c file
	 * @throws IOException
	 */
	public static List<String> readCFile(String fileToRead) throws IOException
	{

		List<String> dependicies = new ArrayList<String>();

		// Makes the file path of the dependencies relative to the root path
		String filePathTemp = Paths.get(fileToRead).toString().substring(0, fileToRead.lastIndexOf("\\")).replace(root, "");

		// Checks if beginning of file has a \. If so remove it.
		if (filePathTemp.startsWith("\\"))
			filePathTemp = filePathTemp.substring(1);

		final String filePath = filePathTemp;

		Files.lines(Paths.get(fileToRead)).forEach(line ->
			{
				if (line.contains("#include ") && ((line.contains("\"")))) //|| line.contains("<"))))
				{
					if (line.contains("\"")) {
                        line = line.replace("#include \"", "");
                        line = line.replace("\"", "");

                        // Checks if the file is a C Standard Library. If so,
                        // does not do anything about it

                        // Checks if #include statement has ""
                        //if (line.contains("/"))
                        //	line = line.replace("/", "\\");
                        dependicies.add(filePath + line);
                        //dependicies.add(filePath + "/" + line);
                    }
					/*
					else
					{
						// Checks if #include statement has <>
						line = line.replace("#include <", "");
						line = line.replace(">", "");
						if (line.contains("/"))
							line = line.replace("/", "\\");

						if (!C_HEADERS.contains(line))
						{
							//dependicies.add(filePath + "/" + line);
							dependicies.add(filePath + line);
						}
					}
					*/
				}
			});

		return dependicies;
	}
}