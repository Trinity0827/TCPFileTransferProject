package top_file_project;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
                    System.out.println("Receiving file upload...");

                    // First, read the remaining data in 'request' buffer
                    ByteArrayOutputStream incomingData = new ByteArrayOutputStream();
                    if (request.hasRemaining()) {
                        incomingData.write(request.array(), request.position(), request.remaining());
                    }

                    // Now read the rest of the data from the channel
                    ByteBuffer fileBuffer = ByteBuffer.allocate(1024);
                    while (serveChannel.read(fileBuffer) > 0) {
                        fileBuffer.flip();
                        incomingData.write(fileBuffer.array(), 0, fileBuffer.limit());
                        fileBuffer.clear();
                    }

                    byte[] completeData = incomingData.toByteArray();

                    // Continue with parsing the filename and file content as before
                    // Locate the first and second delimiters to parse the filename
                    int firstDelimiterIndex = -1;
                    int secondDelimiterIndex = -1;

                    // Scan for the two delimiters '|' in the incoming data
                    for (int i = 0; i < completeData.length; i++) {
                        if (completeData[i] == '|') {
                            if (firstDelimiterIndex == -1) {
                                firstDelimiterIndex = i;
                            } else {
                                secondDelimiterIndex = i;
                                break; // Exit loop after finding the second delimiter
                            }
                        }
                    }

                    if (firstDelimiterIndex != -1 && secondDelimiterIndex != -1) {
                        String uploadFileName = new String(completeData, firstDelimiterIndex + 1, secondDelimiterIndex - firstDelimiterIndex - 1);

                        byte[] fileContentBytes = new byte[completeData.length - secondDelimiterIndex - 1];
                        System.arraycopy(completeData, secondDelimiterIndex + 1, fileContentBytes, 0, fileContentBytes.length);

                        FileOutputStream fos = new FileOutputStream("ServerFiles/" + uploadFileName);
                        fos.write(fileContentBytes);
                        fos.close();

                        String uploadCode;
                        boolean uploadSuccess = new File("ServerFiles/" + uploadFileName).exists();

                        if (uploadSuccess) {
                            System.out.println("File uploaded successfully.");
                            uploadCode = "S";
                        } else {
                            System.out.println("File upload failed.");
                            uploadCode = "F";
                        }

                        ByteBuffer uploadResponseBuffer = ByteBuffer.wrap(uploadCode.getBytes());
                        serveChannel.write(uploadResponseBuffer);
                    } else {
                        System.out.println("Error: Invalid data format received.");
                    }

                    serveChannel.close();
                    break;





                case "N": //download
                    break;
                default:
                    System.out.println("Invalid Command");
            }//switch
        }//while
    }
}
