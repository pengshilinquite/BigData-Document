package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HqlSplitter {

    public static void main(String[] args) {
        String hqlScript = "drop table if exists dm_fin.dm_fin_pl_cust_f;\n" +
                "create table if not exists dm_fin.dm_fin_pl_cust_f\n" +
                "(\n" +
                "id bigint\n" +
                ",name string\n" +
                ")\n" +
                "comment ''\n" +
                "stored as orc;\n" +
                "\n" +
                "drop table if exists dm_temp.dm_fin_pl_cust_f2;\n" +
                "create table if not exists dm_temp.dm_fin_pl_cust_f2\n" +
                "(\n" +
                "id bigint\n" +
                ",name string\n" +
                ")\n" +
                "comment ''\n" +
                "stored as orc;\n" +
                "\n" +
                "drop table if exists dm_temp.dm_fin_pl_cust_f3;\n" +
                "create table if not exists dm_temp.dm_fin_pl_cust_f3\n" +
                "as \n" +
                "select 1 as id,'zhangsan' as name \n" +
                "from dual\n" +
                "union all\n" +
                "select 2 as id, '李四' as name \n" +
                ";\n" +
                "\n" +
                "insert overwrite table dm_temp.dm_fin_pl_cust_f2\n" +
                "select * from dm_temp.dm_fin_pl_cust_f3\n" +
                ";\n" +
                "\n" +
                "drop table if exists dm_fin.dm_fin_pl_sum_f;\n" +
                "create table if not exists dm_fin.dm_fin_pl_sum_f\n" +
                "(\n" +
                "id bigint\n" +
                ",name string\n" +
                ")\n" +
                "comment ''\n" +
                "stored as orc;\n" +
                "\n" +
                "insert overwrite table dm_fin.dm_fin_pl_cust_f\n" +
                "select * from dm_temp.dm_fin_pl_cust_f2\n" +
                ";\n" +
                "\n" +
                "insert overwrite table dm_fin.dm_fin_pl_sum_f\n" +
                "select * from dm_temp.dm_fin_pl_cust_f\n" +
                ";";

        List<String> tables = extractTables(hqlScript);
        List<String> dependencies = extractDependencies(hqlScript);

        System.out.println("Tables:");
        for (String table : tables) {
            System.out.println(table);
        }

        System.out.println("\nDependencies:");
        for (String dependency : dependencies) {
            System.out.println(dependency);
        }
    }

    public static List<String> extractTables(String hqlScript) {
        List<String> tables = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?i)\\b(?:create|insert\\s+overwrite)\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?(\\w+(?:\\.\\w+)?)");
        Matcher matcher = pattern.matcher(hqlScript);
        while (matcher.find()) {
            tables.add(matcher.group(1));
        }
        return tables;
    }

    public static List<String> extractDependencies(String hqlScript) {
        List<String> dependencies = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?i)\\binsert\\s+overwrite\\s+table\\s+(\\w+(?:\\.\\w+)?)");
        Matcher matcher = pattern.matcher(hqlScript);
        while (matcher.find()) {
            dependencies.add(matcher.group(1));
        }
        return dependencies;
    }
}
