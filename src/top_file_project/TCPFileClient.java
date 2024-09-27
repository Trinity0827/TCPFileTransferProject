package top_file_project;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

// byte[] code is only necessary when you expect to receive additional data from the client along with the command, such as a
//filename for delete, rename .In the case of the "L" (list) command,the server only needs to receive the
// command itself ("L") from the client.

public class TCPFileClient {
    public static void main(String[] args) throws Exception {
        if(args.length !=2) {
            System.out.println("Please specify <serverIP> and <serverPort>");
            return;
        }
        int serverPort = Integer.parseInt(args[1]);
        String command;
        System.out.print("Enter your choice (D:Delete, L:List, R:Rename, U:Upload, N:Download) ");



        do { //do -> handle multiple command- 1 at a time
            Scanner keyboard = new Scanner(System.in);
            command = keyboard.nextLine().toUpperCase();

            switch (command) {
                case "D": //delete
                    System.out.println("Please enter the file name:");
                    String fileName = keyboard.nextLine();
                    ByteBuffer request =
                            ByteBuffer.wrap((command+fileName).getBytes());
                    SocketChannel channel = SocketChannel.open();
                    channel.connect(new InetSocketAddress(args[0], serverPort));
                    channel.write(request);
                    channel.shutdownOutput();
                    //TODO: receive server code and tell user if action was success or failure

                    ByteBuffer reply = ByteBuffer.allocate(1);
                    channel.read(reply);
                    reply.flip();
                    byte[]a = new byte[1];
                    reply.get(a);
                    String code = new String(a);
                    if(code.equals("S")) {
                        System.out.println("File successfully deleted");
                    }else if (code.equals("F")) {
                        System.out.println("Failed to delete file");
                    }else {
                        System.out.println("Invalid server code received");
                    }
                    channel.close();
                    break;



                case "L": //list
                    ByteBuffer listRequest = ByteBuffer.wrap(command.getBytes());
                    SocketChannel listChannel = SocketChannel.open();
                    listChannel.connect(new InetSocketAddress(args[0], serverPort));
                    listChannel.write(listRequest);
                    listChannel.shutdownOutput();

                    // This is Reciving the file from server
                    ByteBuffer listReply = ByteBuffer.allocate(1024);  // 1 KB buffer for list
                    listChannel.read(listReply);
                    listReply.flip();
                    byte[] listBytes = new byte[listReply.remaining()];
                    listReply.get(listBytes);
                    String fileList = new String(listBytes);

                    //showing list of files to the user
                    System.out.println("The file listed is:\n" + fileList);
                    listChannel.close();
                    break;


                case "R": //rename
                    System.out.println("Enter the old file name:");
                    String oldFileName = keyboard.nextLine();

                    System.out.println("Enter the new file name:");
                    String newFileName = keyboard.nextLine();

                    // Combine the command "R" with old and new file names, separated by "|"
                    String renameRequest = "R|" + oldFileName + "|" + newFileName;
                    ByteBuffer renameBuffer = ByteBuffer.wrap(renameRequest.getBytes());

                    SocketChannel renameChannel = SocketChannel.open();
                    renameChannel.connect(new InetSocketAddress(args[0], serverPort));

                    // Send the rename command with the old and new file names to the server
                    renameChannel.write(renameBuffer);
                    renameChannel.shutdownOutput();

                    // Receive server's response (success or failure)
                    ByteBuffer renameReply = ByteBuffer.allocate(1);
                    renameChannel.read(renameReply);
                    renameReply.flip();
                    byte[] renameResponse = new byte[1];
                    renameReply.get(renameResponse);
                    String renameCode = new String(renameResponse);

                    if (renameCode.equals("S")) {
                        System.out.println("Operation successful.");
                    } else if (renameCode.equals("F")) {
                        System.out.println("Operation failed.");
                    } else {
                        System.out.println("Invalid server response received.");
                    }

                    renameChannel.close();
                    break;


                case "U": //upload
                    break;


                case "N": //download
                    break;



                default:
                    if(command.equals("Q")) {
                        System.out.println("Invalid Command");
                    }
            }//switch
        } while (!command.equals("Q"));
    }
}
