package com.peng.learning.Service;

import com.peng.learning.Dao.DruidDao;
import com.peng.learning.model.Emp;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmpService {
    public static void main(String[] args) throws Exception {
        DruidDao druidDao = new DruidDao();
        Connection druidConnect = druidDao.getDruidConnect();
        String strAdd = "insert into dm_fin.dm_fin_emp_h_f values(8888,'peng','CLERK',7782,'1996-12-03',1300,null,10);";
        String strUpdate = "update  dm_fin.dm_fin_emp_h_f set comm= 800 where empno = 8888  ";
        String strQuery = "select * from dm_fin.dm_fin_emp_h_f where empno = 8888";
        String strDelete = "delete from dm_fin.dm_fin_emp_h_f where empno = 8888";

        //addExec(strAdd,druidConnect);
        //updateExec(strUpdate,druidConnect);
        //queryExec(strQuery,druidConnect);
        //deleteExec(strDelete,druidConnect);
        Statement statement = druidConnect.createStatement();
        System.out.println(statement.execute(strDelete));


    }

    public static void deleteExec(String sql,Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        boolean execute = statement.execute(sql);
        System.out.println(execute);
        if (execute){
            System.out.println("删除数据成功");

        }else {
            System.out.println("删除数据失败");
        }

    }

    public  static void updateExec(String sql, Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        int i = statement.executeUpdate(sql);
        if (i>=1){
            System.out.println("更新数据成功");
        }else {
            System.out.println("更新数据失败");
        }
    }

    private static void addExec(String sql,Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        boolean execute = statement.execute(sql);
        if (execute){
            System.out.println("插入数据成功");
        }else {
            System.out.println("插入数据失败");
        }

    }

    public static void  queryExec  (String sql,Connection connection) throws Exception {
        //获取数据库连接

        //对sql进行预编译
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery();
        List<Emp> list = new ArrayList<>();
        while (resultSet.next()){
            Emp emp = new Emp();
            emp.setEmpno(resultSet.getInt("empno"));
            emp.setEname(resultSet.getString("ename"));
            emp.setJob(resultSet.getString("job"));
            emp.setMgr(resultSet.getInt("mgr"));
            emp.setHiredate(resultSet.getDate("hiredate"));
            emp.setSal(resultSet.getDouble("sal"));
            emp.setComm(resultSet.getDouble("comm"));
            emp.setDeptno(resultSet.getInt("deptno"));
            list.add(emp);

        }

        System.out.println(list);

    }
}
