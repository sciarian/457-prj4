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

   
class server{
   
          static int port = 9000;
          static String docroot = "";
          static String logfile = "logfile_defualt";
	  static File the_log_file = null;
  
	//GOT THIS FROM STACK OVER FLOW
	public static String get_HTTP_Time() {
 		   Calendar calendar = Calendar.getInstance();
		   SimpleDateFormat dateFormat = new SimpleDateFormat(
       		   "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                   dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    		   return dateFormat.format(calendar.getTime());
	}

	//NOW THIS ON THE OTHER HAND IS COMPLETELY ORIGINAL
	public static String get_Last_Modified(String filename){
		File file = new File(filename);
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                return dateFormat.format(file.lastModified());
	}
	
	public static boolean isMod(File file, String client_is_mod){
		
		String[] tokens = client_is_mod.split(":");
		int secondsToMs = Integer.parseInt(tokens[2]) * 1000;
		int minutesToMs = Integer.parseInt(tokens[1]) * 60000;
		int hoursToMs = Integer.parseInt(tokens[0]) * 3600000;
		long client_mod_time = secondsToMs + minutesToMs + hoursToMs;

		//GRAB LAST TIME FILE MODIFIED
		long lastModTime = file.lastModified();
		
		System.out.println("\nTIME DIFF : " + (client_mod_time - lastModTime) + "\n");
		
		if(client_mod_time - lastModTime > 50000)
			return true;
		
		return false;
	}

        public static byte[] fileToBytes(String filename){
		//CREATE A FILE INPUT STREAM
		FileInputStream fin = null;

		//CREATTE A BYTE ARRAY
		byte[] fileBytes = null;

		//GRAB BYTES FROM FILE
		try{
			//GRAB THE FILE
			File file = new File(filename);

			//MAKE THE BYTE ARRAY THE SIZE OF THE FILE
			fileBytes = new byte[(int) file.length()];

			//OPEN UP A FILE STREAM FOR THE FILE
			fin = new FileInputStream(file);

			//PUT THE FILES BYTES INTO THE BYTE ARRAY WITH THE BYTE STREAM
			fin.read(fileBytes);			

		}catch(IOException e){
			System.out.println("YOU DONE GOOFED");
		}finally{
				try{
					fin.close();
				}
				catch(IOException e){
					System.out.println("CHIMICHANGAS?");
				}
		}

		//ADD CRLF TO THE END OF THE FILE
		String CRLF = "\r\n";
		byte[] CRLF_bytes = CRLF.getBytes();		
		byte[] fileBytes_CRLF = new byte[fileBytes.length + CRLF_bytes.length];
		
		System.arraycopy(fileBytes, 0, fileBytes_CRLF, 0, fileBytes.length);
		System.arraycopy(CRLF_bytes, 0,fileBytes_CRLF, fileBytes.length, CRLF_bytes.length);
	
		//RETURN THE FILE BYTES
		return fileBytes;
	  }

	  public static void respond(ArrayList<String> req, SocketChannel sc){
	
		//<CRLF>
		String CRLF = "\r\n";
		
		//CREATE FILE INPUT STREAM
		
		//FILE TO CHECK IF THE FILE EXIST
		File the_file = null;

		//Srting to contain file headers
		String headers = "";

		System.out.println(docroot + req.get(1).substring(1)); 

		
		try{
	
		 ///////////////////////////////
		 //SEND 501 IF COMMAND NOT GET//
		 ///////////////////////////////
		 if(!req.get(0).equals("GET")){
			String response = "HTTP/1.1 501 Not Implemented" + CRLF;
                        ByteBuffer statusBuf = ByteBuffer.wrap(response.getBytes());
                        sc.write(statusBuf);
                        statusBuf.flip();

                        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        //ADD HEADERS                   
                        headers += response;

                        ///////////////
                        //DATE HEADER
                        String date_hdr = get_HTTP_Time() + CRLF;//Get current time in HTTP format.
                        headers += "Date: " + date_hdr;                  //Add date header to headers.

                        ////////////////////////////
                        //LAST DATE MODIFIED HEADER
                        String last_mod_hdr = get_Last_Modified("file501.html") + CRLF;
                        headers += "Last-modified: " + last_mod_hdr;

                        //////////////////////
                        //CONTENT TYPE HEADER
                        File file = new File("file501.html");
                        String type_hdr = Files.probeContentType(file.toPath()) + CRLF;
                        headers += "Content-type: " + type_hdr;

                        ///////////////////////
                        //CONTENT LENGTH HEADER 
                        String len_hdr = "Content-length: " + file.length() + CRLF;
                        headers += len_hdr;

                        /////////////////////////////////////////////////////////////////////////////////////
                        //SEND HEADERS
                        ByteBuffer hdrBuf = ByteBuffer.wrap(headers.getBytes());
                        sc.write(hdrBuf);
                     
			//SEND ENDING <CRLF>
                        ByteBuffer endHdrBuf = ByteBuffer.wrap(CRLF.getBytes());
                        sc.write(endHdrBuf);
                        endHdrBuf.flip();
		
			/////////////////////////////////////////////////////////////
			//SEND 501 FILE
	                ByteBuffer fileBuf = ByteBuffer.wrap(fileToBytes("file501.html"));
                        sc.write(fileBuf);
                        fileBuf.flip();

		 }
		 else if((the_file = new File(docroot + req.get(1).substring(1))).exists()){

			String response;
			boolean isModFlag = true;
			headers = "";			
	
			//IF CHECK IF THE FILE IS MODIFIED
			for(String r : req)
				if(r.contains("If-Modified-Since"))
					isModFlag = isMod(the_file, req.get(req.indexOf(r) + 5));
	
			if(isModFlag){
				response = "HTTP/1.1 200 Ok" + CRLF;
				headers += response;
			}else{
				response = "HTTP/1.1 304 Not Modified" + CRLF;
				headers += response;
			}

                        ///////////////
                        //DATE HEADER
                        String date_hdr = get_HTTP_Time() + CRLF;	 //Get current time in HTTP format.
                        headers += "Date: " + date_hdr;                  //Add date header to headers.

                        ////////////////////////////
                        //LAST DATE MODIFIED HEADER
                        String last_mod_hdr = get_Last_Modified(the_file.getAbsolutePath()) + CRLF;
                        headers += "Last-modified: " + last_mod_hdr;

                        //////////////////////
                        //CONTENT TYPE HEADER
                        String type_hdr = Files.probeContentType(the_file.toPath()) + CRLF;
                        headers += "Content-type: " + type_hdr;

                        ///////////////////////
                        //CONTENT LENGTH HEADER 
                        String len_hdr = "Content-length: " + the_file.length() + CRLF;
                        headers += len_hdr;
	
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////
                        //SEND HEADERS
                        ByteBuffer hdrBuf = ByteBuffer.wrap(headers.getBytes());
                        sc.write(hdrBuf);
		
		
                        /////////////////////////////
                        //SEND END <CRLF> FOR HEADERS   
                        ByteBuffer endHeaderBuf = ByteBuffer.wrap(CRLF.getBytes());
                        sc.write(endHeaderBuf);
                        endHeaderBuf.flip();
		
			System.out.println("FILE NAME: " + the_file.getName());
	
			//SEND FILE BYTES TO THE CLIENT IF THE FILE IS MODIFIED
			if(isModFlag == true){
                   	     ByteBuffer fileBuf = ByteBuffer.wrap(fileToBytes(the_file.getAbsolutePath()));
                             sc.write(fileBuf);
                             fileBuf.flip();
			}

		 }
		 else{

			//////////////////////////////
			//SEND 404 NOT FOUND TO USER//
			//////////////////////////////
			
			headers = "";
			
			/////////////////////////////////////////////////////////////////////////////////////////////////////////
			//SEND START LINE TO CLIENT
			String response = "HTTP/1.1 404 Not Found" + CRLF;
			ByteBuffer statusBuf = ByteBuffer.wrap(response.getBytes());
               	        sc.write(statusBuf);
               		statusBuf.flip();

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			//ADD HEADERS			
			headers += response;			
	
			///////////////
			//DATE HEADER
			String date_hdr = get_HTTP_Time() + CRLF;//Get current time in HTTP format.
			headers += "Date: " + date_hdr;			 //Add date header to headers.
			
			////////////////////////////
			//LAST DATE MODIFIED HEADER
			String last_mod_hdr = get_Last_Modified("file404.html") + CRLF;
			headers += "Last-modified: " + last_mod_hdr;
			
			//////////////////////
			//CONTENT TYPE HEADER
			File file = new File("file404.html");	
			String type_hdr = Files.probeContentType(file.toPath()) + CRLF;
			headers += "Content-type: " + type_hdr;

			///////////////////////
			//CONTENT LENGTH HEADER	
			String len_hdr = "Content-length: " + file.length() + CRLF;			
			headers += len_hdr;
			
			//////////////////////////////////////////////////////////////////////////////////////////////////////////////
			//SEND HEADERS
			ByteBuffer hdrBuf = ByteBuffer.wrap(headers.getBytes());
			sc.write(hdrBuf);		

			/////////////////////////////
			//SEND END <CRLF> FOR HEADERS	
			ByteBuffer endHeaderBuf = ByteBuffer.wrap(CRLF.getBytes());
                        sc.write(endHeaderBuf);
                        endHeaderBuf.flip();
		
			//SEND FILE BYTES TO CLIENT
			ByteBuffer fileBuf = ByteBuffer.wrap(fileToBytes("file404.html"));		
			sc.write(fileBuf);
			fileBuf.flip();

		}
		
		System.out.println(headers);

             try{
                    
                    FileWriter fw = new FileWriter(logfile,true); //the true will append the new data
                    fw.write(req + "\n");//appends the string to the file
                    fw.close();
             }
             catch(IOException ioe){
                    System.err.println("IOException: " + ioe.getMessage());
             }
		
		
				
	      }catch(IOException e){
			System.out.println("Got a IO Exception in respond");
	      }
}

          public static void main(String[] args){
                  Console cons = System.console();
                  ServerSocketChannel c;
		

		  //DEFUALT VALUES FOR SECURITY VARIABLES
		  port = 9000;
		  
                  try{
                          //CREATES A CONNECTION SOCKET COMMUNICATION. SOCKETS WILL BE ADDED LATER
                          c = ServerSocketChannel.open();
 			  
			  //////////////////////////////////////////////////////////////////
			  //PUT ARGS INTO A ARRAY LIST					  //
			  //////////////////////////////////////////////////////////////////
			  ArrayList<String> arg_list = new ArrayList<String>();
			  for(int x = 0; x <args.length; x++)
				arg_list.add(args[x]);

			  for(String arg : arg_list){
			  	if(arg.equals("-p"))
					port = Integer.parseInt(arg_list.get(arg_list.indexOf(arg)+1));
				if(arg.equals("-docroot"))
					docroot = arg_list.get(arg_list.indexOf(arg)+1);			
				if(arg.equals("-logfile"))
					logfile = arg_list.get(arg_list.indexOf(arg)+1);
			  }
			
			
	                  

		          System.out.println("DOCROOT: " + docroot + " LOGFILE: " + logfile + " PORT: " + port);

                          //BIND SERVER TO THE PORT WE GOT FROM STDIN
                          c.bind(new InetSocketAddress(port));
  
  
                          //////////////////////////////////////////////////////////////////////////////
                          //THROW SERVER SOCKET CHANNEL INTO A THREAD TO CONTINUOUSLY RECV CONNECTIONS//
                          //////////////////////////////////////////////////////////////////////////////
  
                          //CREATE THREAD  
                          NewClientApproaches th = new NewClientApproaches(c);
  
                          //START THREAD
                          th.start();
  
                  }catch (IOException e){
                          System.out.println("Got and IO exception in the main");
                  }
 
  
  
                  while(true){
                          String m = cons.readLine("Enter quit to exit\n");
  
                          //IF SERVER QUITS
                          if(m.contains("quit")){
                                  //CLOSE PROGRAM
                                  System.exit(0);
                          }
                  }
  
          }
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //USE TO CONNECT TO NEW CLIENTS TO THE SERVER 
  class NewClientApproaches extends Thread{
  
          //USED TO GRAB THE SERVERS SOCKET CHANNEL
          ServerSocketChannel c;
  
          //GRAB THE CLIENTS SOCKET CHANNEL
          NewClientApproaches(ServerSocketChannel channel){
                  c = channel;
          }
  
          //RUN THREAD
          public void run(){
                  //LOOP. ACCEPT NEW CLIENT CONNECTIONS, ADD THEM TO THE CLIENT LIST, 
                  try{
                          while(true){
                                  //ACCPET CLIENT CONNECTION                               
                                  SocketChannel client_sc = c.accept();
  
                                  //CREATE A THREAD TO RECIEVE MSG'S FROM THE CLIENT       
                                  TcpServerThread t = new TcpServerThread(client_sc);
  
                                  //START THE THEAD
                                  t.start();
                          }
  
                  }catch(IOException e){
                          System.out.println("Got an IO exception in new client server");
                  }
          }
  }
  
  
  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //RECEIVE MESSAGES FROM CLIENT
  class TcpServerThread extends Thread{
  
     //CREATE SOCKET CHANNEL
     SocketChannel sc;
 
     //CONNECTS CLIENT SOCKET TO THE SERVER SOCKET
     TcpServerThread(SocketChannel channel){
         sc=channel;
     }

     //RUN THREAD
     public void run(){
         try{
             while(true){
 
                 ByteBuffer buffer = ByteBuffer.allocate(4096);
                 sc.read(buffer);
                 buffer.flip();
                 byte[] a = new byte[buffer.remaining()];
                 buffer.get(a);
                 String req = new String(a);
                 System.out.println("");
                 System.out.println("Got from web browser: \n"+ req);
		

		 try
		{
		   
		    FileWriter fw = new FileWriter(server.logfile,true); //the true will append the new data
		    fw.write(req + "\n");//appends the string to the file
		    fw.close();
		}
		catch(IOException ioe)
		{
		    System.err.println("IOException: " + ioe.getMessage());
		}
		 
		 //DELIMIT THE MESSAGE BY NEW LINE, STORE IN A ARRAY LIST
                 //RESETS BUFFER POSTION TO ZERO
                 buffer.rewind();

		 //SPLIT STRING
		 String msg[] = req.split(" ");
		 ArrayList<String> req_list = new ArrayList<String>();
 
		 for(int x = 0; x < msg.length; x++)
		 	req_list.add(msg[x]);
		
	     	// System.out.println(req_list);
				
		 server.respond(req_list,sc);
 
             }
 
         //CATCH IO EXCEPTIONS
         }catch (IOException e){
             System.out.println("Got an IO exception IN TCP SERVER");
         }
     }
 }

