//package org.example;
//
//public class Application {
//    public static void main(String[] args) {
//        Computer computer = new Computer();
//        Person person = new Person();
//        computer.name = "IBM";
//         person.name = "zhangsan";
//        int computer_Result= computer.computer_Play();
//        int person_Result = person.person_Play();
//        if (computer_Result == person_Result){
//            System.out.println("平局");
//        }else if (computer_Result == 0 && person.person_Play() == 2
//                || computer_Result == 1 && person_Result ==0
//                || computer_Result == 2 && person_Result ==1
//        ){
//            System.out.println("电脑赢");
//            computer.score +=10;
//            System.out.println(computer.score);
//        }else{
//            System.out.println();
//            System.out.println("玩家赢");
//            person.score+=10;
//            System.out.println(person.score);
//        }
//
//    }
//}
