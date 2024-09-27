package top_file_project;


import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


public class TCPFilesServer {
    public static void main(String[] args) throws Exception {
        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(3000)); //must match client port


        while (true){
            SocketChannel serveChannel = listenChannel.accept();
            ByteBuffer request = ByteBuffer.allocate(1024);
            int numBytes = serveChannel.read(request); //read request from tcp channel
            request.flip(); //read bytes out of buffer
            byte[] a = new byte[1]; //size of array should match number of bytes in command
            request.get(a);
            String command = new String(a);
            System.out.println("\nreceived command:" + command);


            switch (command){
                case "D": //delete
                    byte[] b = new byte[request.remaining()];
                    request.get(b);
                    String fileName = new String(b);
                    System.out.println("File to delete:" + fileName);
                    File file = new File("ServerFiles/" + fileName);
                    boolean success = false;
                    if(file.exists()) {
                        success = file.delete();
                    }
                    String code;
                    if (success) {
                        System.out.println("Deletion successful");
                        code = "S";
                    } else {
                        System.out.println("Deletion failed");
                        code = "F";
                    }
                    ByteBuffer reply = ByteBuffer.wrap(code.getBytes());
                    serveChannel.write(reply);
                    serveChannel.close();
                    break;

                case "L": //list
                    System.out.println("Listing files...");
                    File folder = new File("ServerFiles");//This is going to read from the folder I made
                    File[] listOfFiles = folder.listFiles();

                    // Collect file names into a string
                    StringBuilder fileList = new StringBuilder();
                    if (listOfFiles != null) {
                        for (File f : listOfFiles) {
                            if (f.isFile()) {
                                fileList.append(f.getName()).append("\n");
                            }
                        }
                    }

                    // Send the list of files back to the client
                    ByteBuffer listReply = ByteBuffer.wrap(fileList.toString().getBytes());
                    serveChannel.write(listReply);
                    serveChannel.close();
                    break;


                case "R": //rename
                    byte[] renameData = new byte[request.remaining()];
                    request.get(renameData);
                    // This will be in the format "R|oldFileName|newFileName"
                    String renameDetails = new String(renameData);

                    // Split the details to get old and new file names
                    String[] fileNames = renameDetails.split("\\|");
                    String oldFileName = fileNames[1];
                    String newFileName = fileNames[2];

                    System.out.println("Renaming file: " + oldFileName + " to " + newFileName);

                    File oldFile = new File("ServerFiles/" + oldFileName);
                    File newFile = new File("ServerFiles/" + newFileName);

                    boolean renameSuccess = false;
                    if (oldFile.exists()) {
                        renameSuccess = oldFile.renameTo(newFile);
                    }

                    String renameCode;
                    if (renameSuccess) {
                        System.out.println("File renamed successfully.");
                        renameCode = "S";
                    } else {
                        System.out.println("Failed to rename file.");
                        renameCode = "F";
                    }

                    ByteBuffer renameReply = ByteBuffer.wrap(renameCode.getBytes());
                    serveChannel.write(renameReply);
                    serveChannel.close();
                    break;


                case "U": //upload
                    break;
                case "N": //download
                    break;
                default:
                    System.out.println("Invalid Command");
            }//switch
        }//while
    }
}
