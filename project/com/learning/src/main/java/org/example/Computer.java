package org.example;

import java.util.Random;

public class Computer {
    String name;
    int score;
    public static int computer_Play(){
        Random random = new Random(3);
        int x = random.nextInt();
        if (x==0){
            System.out.println("0是石头");
            return x;
        }else if(x ==1 ){
            System.out.println("1是剪刀");
            return x;
        }else if (x ==2){
            System.out.println("2是布");
            return x;
        }else {
            System.out.println("游戏越界");
            return -1;

        }

    }
}
