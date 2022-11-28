import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{
    private ArrayList<GestionConnection> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        this.connections = new ArrayList<>();
        this.done = false;
    }

    @Override
    public void run() {
        try{
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                GestionConnection gestcon = new GestionConnection(client);
                connections.add(gestcon);
                pool.execute(gestcon);
            }
        }catch (Exception ex){
            shutdown();
        }
    }
    public void broadcast(String message){
        for(GestionConnection gc: connections){
            if (gc != null){
                gc.sendMessage(message);
            }
        }
    }
    public void shutdown(){
        try {
            done = true;
            if (!server.isClosed()){
                server.close();
            }
            for (GestionConnection gc : connections){
                gc.shutdown();
            }
        }catch (Exception ex) {

        }
    }
    class GestionConnection implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public GestionConnection(Socket client) {
            this.client = client;
        }
        @Override
        public void run() {
            try{
                out = new PrintWriter(client.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("username: ");
                nickname = in.readLine();
                System.out.println(nickname +" connecté");
                broadcast(nickname +" a rejoint le chat!");
                String message;
                while ((message = in.readLine()) != null){
                    if (message.startsWith("/nick ")){
                        String[] messageSplit = message.split(" ",2);
                        if (messageSplit.length == 2){
                            broadcast(nickname +" renamed to "+messageSplit[1]);
                            System.out.println(nickname +" renamed to "+messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Votre pseudo a bien été modifié comme "+nickname);
                        }else {
                            out.println("no nickname");
                        }
                    } else if (message.startsWith("/quit")) {
                        broadcast(nickname +" s'est déconnecté!");
                        shutdown();
                    }else {
                        broadcast(nickname +": " +message);
                    }
                }
            }catch (Exception ex){
                shutdown();
            }
        }
        public void sendMessage(String message){
            out.println(message);
        }
        public void shutdown(){
            try {
                in.close();
                out.close();
                if (!client.isClosed()){
                    client.close();
                }
            } catch (Exception ex) {

            }
        }
    }
    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
