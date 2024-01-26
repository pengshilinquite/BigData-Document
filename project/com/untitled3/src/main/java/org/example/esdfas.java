package org.example;

import java.util.Scanner;

/**
 * @author pengshilin
 * @date 2023/12/21 20:03
 */
public class esdfas {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String line;
        int i = 0;
        String s1 = null;
        String s2 = null;
        while ((line = scanner.nextLine()) !=null){

            i++;
            if (i==1){
                s1 = line;
                continue;
            }
            if (i==2){
                s2 = line;
                boolean result= s1.replace(" ","").equalsIgnoreCase(s2.replace(" ",""));
                i=0;
                if (result){
                    System.out.println("yes");
                }else {
                    System.out.println("no");
                }
            }


        }
    }
}
