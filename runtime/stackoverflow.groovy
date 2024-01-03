
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Scanner;
/**
 *
 * @author Jonathan Ellithorpe <jde@cs.stanford.edu>
 */
public class LDBCGraphLoader {

  private static  boolean SKIP_COMMIT = false;
  private static final long TX_MAX_RETRIES = 1000;

  public static void loadVertices(Graph graph, String filePath,  boolean printLoadingDots, int batchSize, long progReportPeriod) {

    String[] colNames = null;
    boolean firstLine = true;
    Map<Object, Object> propertiesMap;
    def fileNameParts = filePath.split("s.csv");
    String[] pathParts = fileNameParts[0].split("/");
    String entityName = pathParts[pathParts.size() - 1];


    FileInputStream inputStream = null;
    Scanner sc = null;
    try {
      System.out.println(filePath);
      inputStream = new FileInputStream(filePath);
      sc = new Scanner(inputStream, "US-ASCII");

      colNames = sc.nextLine().split(",");

      long lineCount = 0;
      boolean txSucceeded;
      long txFailCount;

      // For progress reporting
      long startTime = System.currentTimeMillis();
      long nextProgReportTime = startTime + progReportPeriod*1000;
      long lastLineCount = 0;

      while (sc.hasNextLine()) {
        if(lineCount > 100000) break;
        String line = sc.nextLine();
        List<String> result = new ArrayList<String>();
        int start = 0;
        boolean inQuotes = false;
        boolean idEmpty =false;
        for (int current = 0; current < line.length(); current++) {
          if(start > current) continue;
          else if (line.charAt(current) == '\"' && !inQuotes) inQuotes = !inQuotes; // toggle state
          else if (current < line.length()-1 && line.charAt(current) == '\"' &&  line.charAt(current+1) == ','&& inQuotes){
            inQuotes = !inQuotes;
            if(result.size() == 0 && start == current){
               idEmpty = true;
               System.out.println("ID EMPTY1" + line);
            }
            result.add(line.substring(start, current+1));
            start = current + 2;
          } else if (line.charAt(current) == ',' && !inQuotes) {
            //if(result.size() == 1 && line.charAt(current+1) != '\"' &&(line.charAt(current+1) != '<' || (line.charAt(current+1) == '<' && line.charAt(current+2) == '/')))  continue;
            if((result.size() == 1 || result.size() == 2 ) && !Character.isDigit(line.charAt(current+1)) && line.charAt(current+1) != '-' && line.charAt(current+1) != '\"' &&(line.charAt(current+1) != '<' || (line.charAt(current+1) == '<' && line.charAt(current+2) == '/')))  continue;
            if(result.size() == 0 && start == current){
               idEmpty = true;
               System.out.println("ID EMPTY2" + line);
            }
            result.add(line.substring(start, current));
            start = current + 1;
          
          }
        }
      if(idEmpty) continue;
      result.add(line.substring(start));
      if(result.size() != colNames.size()){
        System.err.println("invalid separation of csv");
        for(String s: result){
          System.err.println(s);
        }
        continue;
      }
      
      String[] colVals = result.toArray(new String[0]);
      propertiesMap = new HashMap<>();

      for (int j = 0; j < colVals.length; j++) {
        System.out.println(colNames[j]);
        System.out.println(colVals[j]);
          if (colNames[j].contains("ID")) {
            propertiesMap.put("ID", colVals[j]);
          } else if (colNames[j].contains(":")) {
            try {  
              int val = Integer.parseInt(colVals[j]);  
              propertiesMap.put(colNames[j].split(":")[0],val);
              System.out.println("is integer: " + colNames[j].split(":")[0]);
            } catch(NumberFormatException e){   
              propertiesMap.put(colNames[j].split(":")[0],colVals[j]);
            } 
          } else {
            try {  
              int val = Integer.parseInt(colVals[j]);  
              propertiesMap.put(colNames[j],val);
            } catch(NumberFormatException e){   
                  propertiesMap.put(colNames[j],colVals[j]);
            }  
          }
      }
          
      propertiesMap.put(T.label, entityName);

      List<Object> keyValues = new ArrayList<>();
        propertiesMap.forEach{ key, val ->
        keyValues.add(key);
        keyValues.add(val);
      };

      graph.addVertex(keyValues.toArray());
      lineCount++;
    }

    if (sc.ioException() != null) {
          throw sc.ioException();
      }
  } finally {
    if (inputStream != null) 
        inputStream.close();
    if (sc != null) 
        sc.close();
  }
        
}


  public static void loadEdges(Graph graph, String filePath, boolean undirected,
      boolean printLoadingDots, int batchSize, long progReportPeriod,String edgeLabel, int limit)
      throws IOException,  java.text.ParseException {
    long count = 0;
    String[] colNames = null;
    boolean firstLine = true;
    Map<Object, Object> propertiesMap;

	  String startType,endType;
    FileInputStream inputStream = null;
    Scanner sc = null;
    try {
      System.out.println(filePath);
      inputStream = new FileInputStream(filePath);
      sc = new Scanner(inputStream, "US-ASCII");

      String[] verticesTypes = sc.nextLine().split(",");
      for(String vName : verticesTypes){
        if(vName.contains("START")){
          startType = vName.substring(vName.indexOf("(") + 1);
          startType = startType.substring(0,startType.indexOf(")")).toLowerCase();
          System.out.println("start "+ startType);
        }else if (vName.contains("END")){
          endType = vName.substring(vName.indexOf("(") + 1);
          endType = endType.substring(0,endType.indexOf(")")).toLowerCase();
          System.out.println("end "+ endType);
        }
      }
      long lineCount = 0;
      boolean txSucceeded;
      long txFailCount;

      // For progress reporting
      long startTime = System.currentTimeMillis();
      long nextProgReportTime = startTime + progReportPeriod*1000;
      long lastLineCount = 0;

  	
  		while (sc.hasNextLine()) {
  		      String line = sc.nextLine();
  		      if(lineCount > limit) break;
  		      String[] colVals = line.split(",");
            if(colVals.size() != 2) continue;

  		      GraphTraversalSource g = graph.traversal();
  		      def Vertex vertex1;
  		      def Vertex vertex2;
  		      try {
  		      vertex1 =
  		        g.V().hasLabel(startType).has("ID", colVals[0]).next();
  		      vertex2 =
  		        g.V().hasLabel(endType).has("ID", colVals[1]).next();
  		      } catch(Exception e ){
  		        System.err.println(lineCount + ":Missing One of "  + colVals[0] + colVals[1]);
  		        continue;
  		      }
  		      
  		      vertex1.addEdge(edgeLabel, vertex2);

  		      // if (undirected) {
  		      //   vertex2.addEdge(edgeLabel, vertex1);
  		      // }

  		      lineCount++;
            
            System.out.println(lineCount + "Added"  + colVals[0] + "  " + colVals[1]);

  		}
  		// note that Scanner suppresses exceptions
  		if (sc.ioException() != null) {
  		    throw sc.ioException();
  		}
	} finally {
		if (inputStream != null) 
		    inputStream.close();
		if (sc != null) 
		    sc.close();
	}
  }
}

inputBaseDir = "/opt/import"
graph = TinkerGraph.open();
      
System.out.println( "Num nodes " + graph.traversal().V().count().next());
System.out.println( "Num edges " + graph.traversal().E().count().next());

batchSize = 1000
progReportPeriod = 50

  vertexLabels = [  "person",      "comment",      "forum",      "organisation",      "place",      "post",      "tag",      "tagclass" ];

    edgeLabels = [
      "containerOf",
      "hasCreator",
      "hasInterest",
      "hasMember",
      "hasModerator",
      "hasTag",
      "hasType",
      "isLocatedIn",
      "isPartOf",
      "isSubclassOf",
      "knows",
      "likes",
      "replyOf",
      "studyAt",
      "workAt"
    ];

    // All property keys with Cardinality.SINGLE
    singleCardPropKeys = [
      "birthday", // person
      "browserUsed", // comment person post
      "classYear", // studyAt
      "content", // comment post
      "creationDate", // comment forum person post knows likes
      "firstName", // person
      "gender", // person
      "imageFile", // post
      "joinDate", // hasMember
      //"language", // post
      "lastName", // person
      "length", // comment post
      "locationIP", // comment person post
      "name", // organisation place tag tagclass
      "title", // forum
      "type", // organisation place
      "url", // organisation place tag tagclass
      "workFrom", // workAt
    ];

    // All property keys with Cardinality.LIST
    listCardPropKeys = [
      "email", // person
      "language" // person, post
    ];


    nodeFiles = [
      "posts.csv",
      "tags.csv",
      "users.csv"
    ];

    edgeFiles = [
      "posts_rel.csv",
      "tags_posts_rel.csv",
      "users_posts_rel.csv"      
    ];

    try {
      
      for (String fileName : nodeFiles) {
        System.out.print("Loading node file " + fileName + " ");
        try {
          LDBCGraphLoader.loadVertices(graph, inputBaseDir + "/" + fileName,
              true, batchSize, progReportPeriod);
          System.out.println("Finished");
        } catch (NoSuchFileException e) {
          System.out.println(" File not found.");
        }
      }

      graph.createIndex('ID',Vertex.class);


      for (String fileName : edgeFiles) {
        System.out.print("Loading edge file " + fileName + " ");
        try {
          if (fileName.contains("tags")) {
            LDBCGraphLoader.loadEdges(graph,inputBaseDir + "/" + fileName, false,
                true, batchSize, progReportPeriod,"HAS_TAG",40000);
          }else if (fileName.contains("users")) {
            LDBCGraphLoader.loadEdges(graph, inputBaseDir + "/" + fileName, false,
                true, batchSize, progReportPeriod,"POSTED",40000);
          } else {
            LDBCGraphLoader.loadEdges(graph,inputBaseDir + "/" + fileName, false,
                true, batchSize, progReportPeriod,"PARENT_OF",40000);
          }

          System.out.println("Finished");
        } catch (NoSuchFileException e) {
          System.out.println(" File not found.");
        }
      }

      System.out.println("Done Loading!");
      System.out.println( "Num nodes " + graph.traversal().V().count().next());
      System.out.println( "Num edges " + graph.traversal().E().count().next());



      // THESE LINES BELOW WILL GENERATE A GraphSON file
      try  {
          os = new FileOutputStream("/runtime/data/stk.json")
          mapper = mapper = graph.io(graphson()).mapper().create()
          graph.io(IoCore.graphson()).writer().mapper(mapper).create().writeGraph(os, graph)
      } catch (Exception e) {
          System.out.println("Exception: " + e);
          e.printStackTrace();
      }
      // END OF GaphSON Generation


    } catch (Exception e) {
      System.out.println("Exception: " + e);
      e.printStackTrace();
    } finally {
      graph.close();
    }


  System.exit(0);
