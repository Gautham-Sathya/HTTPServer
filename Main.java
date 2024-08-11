

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;;




public class Main {
  public static void main(String[] args) {
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    try{
    String directory = null;
    if(args.length==2 && args[0].equalsIgnoreCase("--directory"))
    directory = args[1];

    serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      while (true) {
      clientSocket = serverSocket.accept();
        System.out.println("accepted new connection");
        Handler handler = new Handler(clientSocket.getInputStream(), clientSocket.getOutputStream(), directory);
        handler.start();
        System.out.println("done");
      }
  }catch (IOException e)
  {
    System.out.println("IOE: " + e);
  }
  }
}




 class Handler extends Thread{
 
  private InputStream input;
  private OutputStream outputStream;
  private String directory;

  public Handler(InputStream input, OutputStream outputStream, String directory)
  {
    this.input = input;
    this.outputStream = outputStream;
    this.directory = directory;
    
  }

  //request handler
  public void run()
{
    try{

    boolean gotFile = false;
    byte[] fileBytes = null;
      final BufferedReader reader = new BufferedReader(new InputStreamReader(input));




//read input and parse as string
      //System.out.println(reader.readLine());
    String s = reader.readLine();
    while(reader.ready())
      {
        s+=reader.readLine();
      }
      System.out.println("String:" + s);
      String outString = "HTTP/1.1 404 Not Found\r\n\r\n";
     
      //System.out.println(s.substring(0,4));
     
     //read and write data sent to server
      if(s.substring(0, 4).equalsIgnoreCase("POST"))
      {
      
       String d = directory;
       String name = s.substring(11, s.substring(12).indexOf(" ")+12);
      String body = s.substring(s.indexOf("application/octet-stream")+24);
      System.out.println("body: " + body);
      System.out.println(d+name);
      String fileName = d+name;
      //System.out.println(fileName);
        File file = new File(fileName);
        File dir = new File(directory);
        if (!dir.exists())
            dir.mkdirs();


        if(!file.exists()){
        file.createNewFile();
        try(FileWriter fileWriter = new FileWriter(file)){
          fileWriter.write(body);}
          System.out.println("finished file creation");
        outString = "HTTP/1.1 201 Created\r\n\r\n";
        }
       else
        outString = "HTTP/1.1 404 Not Found\r\n\r\n";
       
        System.out.println("Sending:" + outString);
       outputStream.write(outString.getBytes());
       outputStream.flush();
       outputStream.close();
       return;
      }
     
     
    else if(s.charAt(5)==' ') // / url path
        outString = "HTTP/1.1 200 OK\r\n\r\n";




    else if(s.substring(5, 9).equals("echo")) //echo string
        {
          String echoString ="";
          for(int i = 10; i<s.length(); i++)
          {
            if(s.charAt(i)==' ')
            break;
            echoString+=s.charAt(i);
          }
          System.out.println(echoString);
          outString = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "+echoString.length()+"\r\n\r\n" + echoString;
        }




    else if(s.substring(5, 15).equals("user-agent")) //user-agent header
        {
          String headerString = "";
          int startIndex = s.indexOf("User-Agent:");
          for(int i = startIndex+12; i<s.length(); i++)
          {
            if(s.charAt(i)=='\r')
            break;
            headerString+=s.charAt(i);
            //System.out.println(s.charAt(i)); //for debugging
          }
          System.out.println(headerString);
          System.out.println(headerString.length());
          outString = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "+headerString.length()+"\r\n\r\n"+headerString ;
        }




    else if(s.substring(5, 10).equals("files")) //files header
        {
          //get file name
          String fileString = "";
          int startIndex = s.indexOf("/files/");
          for(int i = startIndex+7; i<s.length(); i++)
          {
            if(s.charAt(i)==' ')
            break;
            fileString+=s.charAt(i);
            //System.out.println(s.charAt(i));
          }


          //get file path
          Path filePath = Paths.get(directory, fileString);
         
          System.out.println(directory+fileString);
            if(Files.exists(filePath ))
             {
               gotFile = true;
                fileBytes = Files.readAllBytes(filePath);
                outString = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: "+fileBytes.length+"\r\n\r\n" ;

            }
            else
            {
            outString = "HTTP/1.1 404 Not Found\r\n\r\n";
            }
          System.out.println(fileString);
          System.out.println(fileString.length());

        }

        System.out.println("outstrinf: " + outString);
        outputStream.write(outString.getBytes());




        if(gotFile)
        outputStream.write(fileBytes);
        outputStream.flush();
        outputStream.close();
      }
      catch(IOException e)
      {
        throw new RuntimeException(e);
      }
  }


}
