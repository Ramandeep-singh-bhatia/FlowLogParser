import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class FlowLogParser {
    private Map<String, String> tagLookupMap = new HashMap<>();
    private Map<Integer, String> protocolMap = new HashMap<>();
    private List<int[]> unassignedRanges = new ArrayList<>();
    private Map<String, Integer> tagCountsMap = new HashMap<>();
    private Map<String, Integer> portProtocolCountMap = new HashMap<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Generate a timestamp for unique file names
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        // Get the current working directory
        String currentDirectory = System.getProperty("user.dir");

        // Default file names with the current directory path
        String defaultLookupTableFile = currentDirectory + File.separator + "lookup_table.csv";
        String defaultProtocolMapFile = currentDirectory + File.separator + "protocol-numbers.csv";
        String defaultFlowLogsFile = currentDirectory + File.separator + "flow_logs.txt";
        String outputFile = currentDirectory + File.separator + "output_" + timeStamp + ".txt";
        String errorLogFile = currentDirectory + File.separator + "error_log_" + timeStamp + ".txt";

        System.out.print("Enter the path for the lookup table CSV file (default: " + defaultLookupTableFile + "): ");
        String lookupTableFile = scanner.nextLine().trim();
        if (lookupTableFile.isEmpty()) {
            lookupTableFile = defaultLookupTableFile;
        }

        System.out.print("Enter the path for the protocol numbers CSV file (default: " + defaultProtocolMapFile + "): ");
        String protocolMapFile = scanner.nextLine().trim();
        if (protocolMapFile.isEmpty()) {
            protocolMapFile = defaultProtocolMapFile;
        }

        System.out.print("Enter the path for the flow logs text file (default: " + defaultFlowLogsFile + "): ");
        String flowLogsFile = scanner.nextLine().trim();
        if (flowLogsFile.isEmpty()) {
            flowLogsFile = defaultFlowLogsFile;
        }

        FlowLogParser parser = new FlowLogParser();

        try {
            // Validate input files
            parser.validateFile(lookupTableFile, "lookup table");
            parser.validateFile(protocolMapFile, "protocol map");
            parser.validateFile(flowLogsFile, "flow logs");

            // Load and process files
            parser.loadLookupTable(lookupTableFile);
            parser.loadProtocolMap(protocolMapFile);
            parser.parseFlowLogs(flowLogsFile, errorLogFile);
            parser.writeOutput(outputFile);

        } catch (Exception e) {
            String errorMessage = "An unexpected error occurred: " + e.getClass().getName() + " in method '" + e.getStackTrace()[0].getMethodName() + "'. Details: " + e.getMessage();
            // In case of an error generate error log file and only update headers in output file
            parser.writeErrorLog(errorLogFile, errorMessage); 
            parser.writeErrorToOutput(outputFile); // Write to output file with only headers
        }
    }

    // Method to validate if the input files exist in the given path and we have access to the file
    private void validateFile(String fileName, String fileDescription) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            String errorMessage = "Error: The " + fileDescription + " file '" + fileName + "' does not exist.";
            throw new FileNotFoundException(errorMessage);
        }
        if (!file.canRead()) {
            String errorMessage = "Error: The " + fileDescription + " file '" + fileName + "' cannot be read.";
            throw new IOException(errorMessage);
        }
    }

    /*
	Method to process lookup table file to get dstPort, protocol and tag and load it to a map
	Use this map to get the tag associated with the dstport and protocol combination and get tag count
    */
    private void loadLookupTable(String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    String dstPort = parts[0].trim();
                    String protocol = parts[1].trim().toLowerCase();
                    String tag = parts[2].trim();
                    String key = dstPort + "-" + protocol;
                    tagLookupMap.put(key, tag);
                }
            }
        } catch (IOException e) {
            throw new IOException("Failed to load lookup table from '" + fileName + "': " + e.getMessage(), e);
        }
    }

    /*
	Method to process protocol numbers to get the network protocol name from the number in flow log
	Added extra processing for the range associated for Unassigned protocol
	Load the protocol number to name mapping in protocolMap
    */
    private void loadProtocolMap(String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            reader.readLine(); // Skip header line
            while ((line = reader.readLine()) != null) {
                if (line.matches("^\\d+.*") || line.matches("^\\d+-\\d+.*")) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        String protocolField = parts[0].trim();
                        String protocolName = parts[1].trim().toLowerCase();
                        if (protocolField.contains("-")) {
                            String[] rangeParts = protocolField.split("-");
                            int start = Integer.parseInt(rangeParts[0].trim());
                            int end = Integer.parseInt(rangeParts[1].trim());
                            unassignedRanges.add(new int[]{start, end});
                        } else {
                            int protocolNumber = Integer.parseInt(protocolField);
                            protocolMap.put(protocolNumber, protocolName);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new IOException("Failed to load protocol map from '" + fileName + "': " + e.getMessage(), e);
        }
    }

    /*
	Method to parse flow logs. Assumption is that the Destination port is in 7th columns and protocol number is in 8th column in the log
	Use the combination of dstPort and protocol Name to see the tags in lookup table file and also get the count of the tags for the output
    */
    private void parseFlowLogs(String fileName, String errorLogFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");

		// Check if the version column is not version 2, skip and log error
                if (parts.length > 0 && !"2".equals(parts[0])) {
                    String errorMessage = "Program only process flow log with version 2. Skipped record with version " + parts[0] + ": " + line;
                    writeErrorLog(errorLogFile, errorMessage);
                    continue;
                }

                if (parts.length >= 8) {
                    String dstPort = parts[6];
		    String protocolField = parts[7];

		    // Skip the record if dstPort or protocolField is "-"
                    if ("-".equals(dstPort) || "-".equals(protocolField)) 
                    	    continue;
                    try {
                    	int protocolNumber = Integer.parseInt(protocolField);
                    	String protocolName = protocolMap.getOrDefault(protocolNumber, isInUnassignedRange(protocolNumber) ? "unassigned" : "unknown");
                    	String portProtocolKey = dstPort + "-" + protocolName;
                    	String tag = tagLookupMap.getOrDefault(portProtocolKey, "Untagged");
                    	tagCountsMap.put(tag, tagCountsMap.getOrDefault(tag, 0) + 1);
                    	portProtocolCountMap.put(portProtocolKey, portProtocolCountMap.getOrDefault(portProtocolKey, 0) + 1);
		    } catch (NumberFormatException e) {
                        String errorMessage = "Invalid protocol field '" + protocolField + "' in line: " + line;
                        writeErrorLog(errorLogFile, errorMessage);
                    }
                }
            }
        } catch (IOException e) {
            throw new IOException("Failed to parse flow logs from '" + fileName + "': " + e.getMessage(), e);
        }
    }

    // In case of any runtime error create an error file with the associated error
    private void writeErrorLog(String errorLogFile, String errorMessage) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(errorLogFile, true))) {
            writer.write("ERROR: " + errorMessage + "\n");
        } catch (IOException e) {
            System.err.println("Failed to write to error log: " + e.getMessage());
        }
    }

    // Write only the headers to the output file in case of an error
    private void writeErrorToOutput(String outputFile) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("Tag Counts:\nTag,Count\n");
            writer.write("\nPort/Protocol Combination Counts:\nPort,Protocol,Count\n");
        } catch (IOException e) {
            System.err.println("Failed to write to output log: " + e.getMessage());
        }
    }

    // Check if protocol is in unassigned range
    private boolean isInUnassignedRange(int protocol) {
        for (int[] range : unassignedRanges) {
            if (protocol >= range[0] && protocol <= range[1]) {
                return true;
            }
        }
        return false;
    }

    // Write the output to the file
    private void writeOutput(String fileName) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("Tag Counts:\nTag,Count\n");
            for (Map.Entry<String, Integer> entry : tagCountsMap.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }

            writer.write("\nPort/Protocol Combination Counts:\nPort,Protocol,Count\n");
            for (Map.Entry<String, Integer> entry : portProtocolCountMap.entrySet()) {
                String[] parts = entry.getKey().split("-");
                writer.write(parts[0] + "," + parts[1] + "," + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            throw new IOException("Failed to write output to '" + fileName + "': " + e.getMessage(), e);
        }
    }
}
