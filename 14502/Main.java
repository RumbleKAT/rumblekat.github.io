import java.util.*;
import java.io.*;

class Node {
    public int y;
    public int x;

    Node(int y, int x){
        this.y = y;
        this.x = x;
    }
}

class Main{
    
    static int [][] map;
    static int [][] score;
    static boolean [][] visited;
    static int N, M;
    static int [][] dir = {{0,1},{0,-1},{1,0},{-1,0}};
    static ArrayList<Node> emptyList;
    static ArrayList<Node> virusList;
    static int max;
    public static void main(String [] args) throws Exception{
        System.setIn(new FileInputStream("./sample.txt"));
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringTokenizer token = new StringTokenizer(br.readLine());
        N = Integer.parseInt(token.nextToken());
        M = Integer.parseInt(token.nextToken());

        map = new int[N+1][M+1];
        emptyList = new ArrayList<>();
        virusList = new ArrayList<>();
        max = 0;

        for(int i=1;i<=N;i++){
            token = new StringTokenizer(br.readLine());
            for(int j=1;j<=M;j++){
                map[i][j] = Integer.parseInt(token.nextToken());
                if(map[i][j] == 0){
                    emptyList.add(new Node(i,j));
                }else if(map[i][j] == 2){
                    virusList.add(new Node(i,j));
                }
            }
        }
        
        // set combine 
       combine(0,new int [3], 0); 
       System.out.println(max);
    }
    public static void combine(int start, int [] walls, int depth){
        if(depth == 3){
            int [][] newMap = copyArea(map);
            for(int i=0;i<walls.length;i++){
                Node pos = emptyList.get(walls[i]);
                newMap[pos.y][pos.x] = 1;
            }
            executeVirus(newMap);
            max = Math.max(max, calculateMap(newMap));
            return;
        }
        for(int i = start; i< emptyList.size();i++){
            walls[depth] = i;
            combine(i+1, walls, depth+1); // recursive 0 -> 1,2,3,4,... -> 1,2,3,4.... 
        }
    }

    public static void printMap(int [][] map){
        for(int i=1;i<map.length;i++){
            for(int j=1;j<map[0].length;j++){
                System.out.print(map[i][j]+ " ");
            }
            System.out.println();
        }
    }

    public static int [][] copyArea(int [][] map){
        int [][] newMap = new int[map.length][map[0].length];
        for(int i=0;i<newMap.length;i++){
            for(int j=0;j<newMap[0].length;j++){
                newMap[i][j] = map[i][j];
            }
        }
        return newMap;
    }

    public static void executeVirus(int [][] resource){
        Queue<Node> currentVirus = new LinkedList<>();
        for(Node node: virusList){
            currentVirus.add(node);
        }

        while(!currentVirus.isEmpty()){
            Node current = currentVirus.poll();

            for(int i=0;i<dir.length;i++){
                int nextY = dir[i][0] + current.y;
                int nextX = dir[i][1] + current.x;
                
                if(nextY >= 1 && nextY <= resource.length -1 && nextX >= 1 && nextX <= resource[0].length-1){
                    if(resource[nextY][nextX] == 0){
                        resource[nextY][nextX] = 2;
                        currentVirus.add(new Node(nextY,nextX));
                    }
                }
            }
        }
    }

    public static int calculateMap(int [][] resource){
        int count = 0;
        for(int i=1;i<resource.length;i++){
            for(int j=1;j<resource[0].length;j++){
                if(resource[i][j] == 0){
                    count++;
                }
            }
        }
        return count;
    }

}