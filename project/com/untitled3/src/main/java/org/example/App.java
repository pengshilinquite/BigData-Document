package org.example;

import java.util.Scanner;

public class App {
    public static void main( String[] args )  {
        Scanner scanner1 = new Scanner(System.in);
        Scanner scanner2 = new Scanner(System.in);
        Scanner scanner3 = new Scanner(System.in);
        Scanner scanner4 = new Scanner(System.in);
        String s1 = scanner1.nextLine();
        String s2 = scanner2.nextLine();
        String s3 = scanner3.nextLine();
        String s4 = scanner4.nextLine();

        String s1Replace = s1.replace(" ", "");
        String s2Replace = s2.replaceAll(" ","");
        String s3Replace = s3.replaceAll(" ", "");
        String s4Replace = s4.replaceAll(" ","");
        boolean result1;
        boolean result2;

        result1 = s1Replace.equalsIgnoreCase(s2Replace);

        if(result1){
            System.out.println("yes");
        }else{
            System.out.println("no");
        }

        result2 = s3Replace.equalsIgnoreCase(s4Replace);

        if(result2){
            System.out.println("yes");
        }else{
            System.out.println("no");
        }






    }

}
