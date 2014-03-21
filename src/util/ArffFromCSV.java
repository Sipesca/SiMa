package sipesca.utils.format;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ArffFromCSV {
	static void Convert(String inputFilename) {
		Convert(inputFilename, inputFilename.substring(0, inputFilename.length()-".csv".length())+".arff");
	}
	static void Convert(String inputFilename, String outputFilename) {
		if (!inputFilename.endsWith(".csv")) {
			System.out.println("Error converting to Arff: The file name must end with the '.csv' extension");
			return;
		}
		try {
		
			Path FROM = Paths.get(inputFilename);
	        Path TO = Paths.get(outputFilename);
	    
	        CopyOption[] options = new CopyOption[]{
	          StandardCopyOption.REPLACE_EXISTING,
	          StandardCopyOption.COPY_ATTRIBUTES
	        };
	        Files.copy(FROM, TO, options);
			
	        
	        RandomAccessFile out = new RandomAccessFile(new File(outputFilename), "rw");
	        out.seek(0); 
			out.write("@RELATION pasos\n".getBytes());
			out.write("@ATTRIBUTE fecha DATE \"yyyy-MM-dd HH:mm:ss\"\n".getBytes());
			out.write("@ATTRIBUTE pasos  NUMERIC\n\n".getBytes());
			out.write("@data".getBytes());
			out.close();
			
			
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
	
	}
}
