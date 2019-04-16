
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Paths; 
import java.nio.channels.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Locale;


//Austin Van Kempen and Matthew Stone
//Data Comm Project 4

   
class proj4{
   
          static int port = 9000; //keep at 9000 for testing purposes
          static String docroot = "";
          static String logF = "logfile_defualt";
	   static File the_log_file = null;
  
	//This was directly from online. Stack overflow copy and paste.
	public static String get_time() {
           Calendar cal = null; 
 		   cal = cal.getInstance();
		   SimpleDateFormat dateFormat = new SimpleDateFormat(
       		   "EEE, dd MMM yyyy HH:mm:ss z", Locale.US); //Date format with timezone 
                   dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    		   return dateFormat.format(cal.getTime());
	}

	public static String last_Mod(String filename){
		File file = new File(filename);
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                return dateFormat.format(file.lastModified());
}
	
	public static boolean is_Mod(File file, String client_is_mod){
		
		String[] tokens = client_is_mod.split(":");
		int secToMs = Integer.parseInt(tokens[2]) * 1000; //conversion from seconds to milliseconds
		int minToMs = Integer.parseInt(tokens[1]) * 60000; //conversion minutes to milliseconds
		int hoursToMs = Integer.parseInt(tokens[0]) * 3600000; //conversion hours to milliseconds
		long total_time = secToMs + minToMs + hoursToMs; //add all that crap together

		//This takes the file from the last time it had been modified
		long fileMod = file.lastModified();
		
		System.out.println("Time Difference: " + (total_time - fileMod) + "\n");
		
		if(total_time - fileMod > 50000) //specified time
			return true;
		
		return false; //return false if it doesn\"92t meet requirements
	}

        public static byte[] fileToBytes(String filename){
		
		FileInputStream fileIS = null;
		byte[] fileBytes = null;

		try{
			//GRAB THE FILE
			File file = new File(filename);
			fileIS = new FileInputStream(file); 		//open file stream
			fileIS.read(fileBytes);					// put file bytes into array with byte stream
			fileBytes = new byte[(int) file.length()]; //make bit array size of file match up


		}catch(IOException e){
			System.out.println("Error. You messed up man.");
		}finally{
				try{
					fileIS.close();
				}
				catch(IOException e){
					System.out.println("Error. You messed up man."); //all of this is on stack overflow
				}
		}

		//String for CRLF so that HTTP responses and requests are properly formatted.
		// A lot of things end with the CeRLF, every header of the HTTP response and request should end with CRLF

		String CRLF = "\r\n"; //This is how you properly format it
		byte[] CRLF_b = CRLF.getBytes();
		byte[] fb_CRLF = new byte[fileBytes.length + CRLF_b.length];
		
	
		
		System.arraycopy(fileBytes, 0, fb_CRLF, 0, fileBytes.length);
		System.arraycopy(CRLF_b, 0,fb_CRLF, fileBytes.length, CRLF_b.length);
	

	
		return fileBytes; // returns file bytes if you couldn\"92t understand what this line of code did.
	  }

		//NEXT PARTT!!!!!
	  public static void respond(ArrayList<String> req, SocketChannel sc){
	
		
		String CRLF = "\r\n"; //Again, this is for formatting	
		File file_exist = null; 	//checks to see if the file is actually there
		String headers = ""; //String for the headers
		System.out.println(docroot + req.get(1).substring(1)); 
		
		try{
	
	//501 command code
		 if(!req.get(0).equals("GET")){
			String resp = "HTTP/1.1 501 Not Implemented" + CRLF;
                        ByteBuffer statusBuffer = ByteBuffer.wrap(resp.getBytes());
                        sc.write(statusBuffer);
                        statusBuffer.flip();              
                        String header_Date = get_time() + CRLF;//Get current time in HTTP format.
                        String last_Header = last_Mod("file501.html") + CRLF;
                        File file = new File("file501.html");
                        String header_type = Files.probeContentType(file.toPath()) + CRLF;
                        String header_Length = "Content-length: " + file.length() + CRLF; 
					  
						//prints everything
					   headers = resp + "Date: " + header_Date + "Last-modified: " + last_Header + "Content-type: " + header_type + header_Length;

                        //Send out the header(s)
                        ByteBuffer header_Buf = ByteBuffer.wrap(headers.getBytes());
                        sc.write(header_Buf);
                     

                        ByteBuffer endheader_Buf = ByteBuffer.wrap(CRLF.getBytes());   		//sends the final CRLF at the end
                        sc.write(endheader_Buf);
                        endheader_Buf.flip();
		
			
	                ByteBuffer fileBuf = ByteBuffer.wrap(fileToBytes("file501.html"));		//sends the 501 file
                        sc.write(fileBuf);
                        fileBuf.flip();

		 }
		 else if((file_exist = new File(docroot + req.get(1).substring(1))).exists()){
			 boolean isModified = true; //sets it to true
			String resp; //response

		headers = "";	 		

			for(String x : req) 						//checks if file is modified
				if(x.contains("If-Modified-Since"))
					isModified = is_Mod(file_exist, req.get(req.indexOf(x) + 5));
	
			if(isModified){
				resp = "HTTP/1.1 200 Ok" + CRLF;  //bueno
				headers += resp; //response
			}else{
				resp = "HTTP/1.1 304 Not Modified" + CRLF; // not Bueno but also not no Bueno
				headers += resp; //response
			}

                     
                        String header_Date = get_time() + CRLF;	 								//HTTP current time
                        String last_Header = last_Mod(file_exist.getAbsolutePath()) + CRLF;
                        String header_Type = Files.probeContentType(file_exist.toPath()) + CRLF;
                        String header_Length = "Content-length: " + file_exist.length() + CRLF;
                       

					   headers = "Date: " + header_Date + "Last-modified: " + last_Header + "Content-type: " + header_Type + header_Length;


                        ByteBuffer header_Buf = ByteBuffer.wrap(headers.getBytes());  // sends headers
                        sc.write(header_Buf);
		 
                        ByteBuffer endHeaderBuf = ByteBuffer.wrap(CRLF.getBytes());  	// send the final CRLF
                        sc.write(endHeaderBuf);
                        endHeaderBuf.flip();
    
			System.out.println("File Name: " + file_exist.getName());
	
			
			if(isModified == true){
                   	     ByteBuffer fileBuf = ByteBuffer.wrap(fileToBytes(file_exist.getAbsolutePath())); //if modified, send bytes to the client
                             sc.write(fileBuf);
                             fileBuf.flip();
			}

		 }
		 else{

									//     ~~~~~Start of the 404 error~~~~~~
			
			headers = ""; //headers
			String resp = "HTTP/1.1 404 Not Found" + CRLF;
			ByteBuffer statusBuffer = ByteBuffer.wrap(resp.getBytes());
               	        sc.write(statusBuffer);
               		statusBuffer.flip();
			
			String header_Date = get_time() + CRLF;//Get current time in HTTP format.
			String last_Header = last_Mod("file404.html") + CRLF;    						//last date modified			
			File file = new File("file404.html");											// content type
			String header_Type = Files.probeContentType(file.toPath()) + CRLF;
			String header_Length = "Content-length: " + file.length() + CRLF;	 			//content length		
			
			headers = resp + "Date: " + header_Date + "Last-modified: " + last_Header + "Content-type: " + header_Type + header_Length;
	
			ByteBuffer header_Buf = ByteBuffer.wrap(headers.getBytes());  //send out the headers
			sc.write(header_Buf);		

			ByteBuffer endHeaderBuf = ByteBuffer.wrap(CRLF.getBytes());	//again, send final calf 
                        sc.write(endHeaderBuf);
                        endHeaderBuf.flip();
		
			ByteBuffer fileBuf = ByteBuffer.wrap(fileToBytes("file404.html"));		//send the bytes to client
			sc.write(fileBuf);
			fileBuf.flip();

		}
		

		System.out.println(headers);

             try{
                    
                    FileWriter fileWrite = new FileWriter(logF,true); //appends the data
                    fileWrite.write(req + "\n");
                    fileWrite.close();
                }
             catch(IOException ioe)
                {
                    System.err.println("IOException: " + ioe.getMessage()); //IOException catch 
                }
		
    
				
		}catch(IOException e){
			System.out.println("Input Output error!!!!");
		}
	  }

          public static void main(String[] args){
                  Console cons = System.console();
                  ServerSocketChannel con;
		

		  
		  port = 9000; //ITS OVER 9000!!!!!!!!!!!!!!!!!!! Or well exactly 9000
		  
                  try{
                          con = ServerSocketChannel.open(); //connects to the socket
 			  
			
			  ArrayList<String> i_am_tired_list = new ArrayList<String>();
			  for(int x = 0; x <args.length; x++) 
				i_am_tired_list.add(args[x]);

			  for(String arg : i_am_tired_list){
			  	if(arg.equals("-p"))
					port = Integer.parseInt(i_am_tired_list.get(i_am_tired_list.indexOf(arg)+1)); // if user puts custom port number
				if(arg.equals("-docroot"))
					docroot = i_am_tired_list.get(i_am_tired_list.indexOf(arg)+1);				// if user puts document root	
				if(arg.equals("-logF"))
					logF = i_am_tired_list.get(i_am_tired_list.indexOf(arg)+1);					// if user puts in a custom log file
			  }
			
			
	                  

		          System.out.println("DOCROOT: " + docroot + " LOGFILE: " + logF + " PORT: " + port);

                          
                          con.bind(new InetSocketAddress(port)); //bind server to port
                          ClientConn thread1 = new ClientConn(con); //creates the thread
  
                          thread1.start();										  // starting thread for the first client to connect
  
                  }catch (IOException e){
                          System.out.println("Got and Input Output exception :(");
                  }
 
  
  
                  while(true){
                          String m = cons.readLine("Enter quit if you would like to exit.\n");
  
                          
                          if(m.contains("quit")){ 			//if statement saying to close the program if someone enters quit
                                  System.exit(0);			//closes program
                          }
                  }
  
          }
  }
  

  class ClientConn extends Thread{
  
         
          ServerSocketChannel x;
          ClientConn(ServerSocketChannel channel){
                  x = channel;
          }
  
          public void run(){
                  try{
                          while(true){
                                                     
                                  SocketChannel sockC = x.accept();    
                                  TcpServerThread tst = new TcpServerThread(sockC);		//creates thread
                                  tst.start(); 											//start the thread
                          }
  
                  }catch(IOException e){
                          System.out.println("Got an Input Output exception :( ");
                  }
          }
  }
  
  
  class TcpServerThread extends Thread{
  
     SocketChannel sc;

     TcpServerThread(SocketChannel channel){						//connects client and server sockets
         sc=channel;
     }


     public void run(){
         try{
             while(true){
 
                 ByteBuffer buff = ByteBuffer.allocate(4096);					// Basically, makes a thread between client and server to get data
                 sc.read(buff);												// from the web browser and prints a standard output for debugging
                 buff.flip();													// purposes.
                 byte[] me = new byte[buff.remaining()];
                 buff.get(me);
                 String request = new String(me);
                 System.out.println("");
                 System.out.println("Received from the web browser: \n"+ request);
		

		 try
		{
		   
		    FileWriter fileWrite = new FileWriter(proj4.logF,true); //the true will append the new data
		    fileWrite.write(request + "\n"); 							 //appends the string to the file
		    fileWrite.close();
		}
		catch(IOException ioe)
		{
		    System.err.println("IOException: " + ioe.getMessage());
		}	 
                 buff.rewind();			//Its rewind time

		 String msg[] = request.split(" "); //Splits it

		 ArrayList<String> request_list = new ArrayList<String>();
 
		 for(int x = 0; x < msg.length; x++)
		 	request_list.add(msg[x]);
		
				
		 server.respond(request_list,sc);
 
             }
         }catch (IOException e){
             System.out.println("Got an Input Output exception in the TCP Server"); //catches IO exceptions
         }
     }
 }
