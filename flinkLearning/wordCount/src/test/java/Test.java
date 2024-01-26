import java.sql.Timestamp;

import static java.lang.Math.floor;

/**
 * @author pengshilin
 * @date 2023/5/22 23:08
 */
public class Test {
    public static void main(String[] args) {
        System.out.println(new Timestamp(1684768325249L));
        System.out.println(new Timestamp(1684768324249L));
        System.out.println(new Timestamp(1684768323249L));
        System.out.println(new Timestamp(1684768322249L));
        System.out.println(new Timestamp(1684768321249L));
        System.out.println(new Timestamp(1684768320249L));
        System.out.println(new Timestamp(1684768319249L));
        System.out.println(new Timestamp(1684768318249L));
        System.out.println(new Timestamp(1684768317249L));
        System.out.println(new Timestamp(1684768316249L));
        System.out.println(new Timestamp(1684768323000L));  //2023-05-22 23:12:03.0
        System.out.println(new Timestamp(1684768324000L)); //2023-05-22 23:12:04.0
/*        2023-05-22 23:12:05.249
        2023-05-22 23:12:04.249
        2023-05-22 23:12:03.249
        2023-05-22 23:12:02.249
        2023-05-22 23:12:01.249
        2023-05-22 23:12:00.249
        2023-05-22 23:11:59.249
        2023-05-22 23:11:58.249
        2023-05-22 23:11:57.249
        2023-05-22 23:11:56.249*/
        System.out.println(Long.valueOf(8000));
        System.out.println(floor(9000 / 3000) * 3000);
        System.out.println(9000 - (9000 + 3000) % 3000);
    }
}
