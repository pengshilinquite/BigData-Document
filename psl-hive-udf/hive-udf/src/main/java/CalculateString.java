import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * @author pengshilin
 * @date 2023/12/22 0:28
 */

public class CalculateString extends GenericUDF {

    public ObjectInspector initialize(ObjectInspector[] objectInspectors) throws UDFArgumentException {
        return null;
    }

    public Object evaluate(DeferredObject[] deferredObjects) throws HiveException {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine se = scriptEngineManager.getEngineByName("js");
        String s1 = "2 + 5 * 6 * ( 6 - 2 )";
        String s2 = "13 > 14 && 15 < 20";

        return null;
    }

    public String getDisplayString(String[] strings) {
        return null;
    }
}
