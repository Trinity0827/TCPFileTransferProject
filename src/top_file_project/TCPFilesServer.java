package top_file_project;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

                    ByteArrayOutputStream uploadData = new ByteArrayOutputStream();
                    if (request.hasRemaining()) {
                        uploadData.write(request.array(), request.position(), request.remaining());
                    }

                    ByteBuffer uploadDataBuffer = ByteBuffer.allocate(1024);
                    while (serveChannel.read(uploadDataBuffer) > 0) {
                        uploadDataBuffer.flip();
                        uploadData.write(uploadDataBuffer.array(), 0, uploadDataBuffer.limit());
                        uploadDataBuffer.clear();
                    }

                    byte[] completeUploadData = uploadData.toByteArray();

                    int delIdx1 = -1;
                    int delIdx2 = -1;

                    for (int i = 0; i < completeUploadData.length; i++) {
                        if (completeUploadData[i] == '|') {
                            if (delIdx1 == -1) {
                                delIdx1 = i;
                            } else {
                                delIdx2 = i;
                                break;
                            }
                        }
                    }

                    if (delIdx1 != -1 && delIdx2 != -1) {
                        String uploadFileName = new String(completeUploadData, delIdx1 + 1, delIdx2 - delIdx1 - 1);

                        byte[] uploadContentBytes = new byte[completeUploadData.length - delIdx2 - 1];
                        System.arraycopy(completeUploadData, delIdx2 + 1, uploadContentBytes, 0, uploadContentBytes.length);

                        FileOutputStream fos = new FileOutputStream("ServerFiles/" + uploadFileName);
                        fos.write(uploadContentBytes);
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


                case "N": // download
                    System.out.println("Receiving file download...");

                    byte[] downloadData = new byte[request.remaining()];
                    request.get(downloadData);
                    String downloadRequest = new String(downloadData);

                    String[] downloadParts = downloadRequest.split("\\|");
                    if (downloadParts.length < 2) {
                        System.out.println("Invalid format.");
                        ByteBuffer badResponseBuffer = ByteBuffer.wrap("F".getBytes());
                        serveChannel.write(badResponseBuffer);
                        serveChannel.close();
                        break;
                    }

                    String downloadFileName = downloadParts[1];
                    File fileToSend = new File("ServerFiles/" + downloadFileName);

                    if (fileToSend.exists() && fileToSend.isFile()) {
                        System.out.println("Sending file: " + downloadFileName);

                        ByteBuffer downloadSuccess = ByteBuffer.wrap("S".getBytes());
                        serveChannel.write(downloadSuccess);

                        FileInputStream fis = new FileInputStream(fileToSend);
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) > 0) {
                            ByteBuffer fileContentBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                            serveChannel.write(fileContentBuffer);
                        }
                        fis.close();
                        System.out.println("File sent successfully.");
                    } else {
                        System.out.println("File not found: " + downloadFileName);
                        ByteBuffer downloadFail = ByteBuffer.wrap("F".getBytes());
                        serveChannel.write(downloadFail);
                    }
                    serveChannel.close();
                    break;

                default:
                    System.out.println("Invalid Command");
            }//switch
        }//while
    }
}
