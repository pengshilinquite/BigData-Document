package com.huawei.mybatis.mappers;

import com.huawei.mybatis.popj.Class;
import org.apache.ibatis.annotations.Param;

public interface ClassMapper {

    Class   getClassByStepTwo(@Param("cid") Integer cid);

    Class   getClass(@Param("cid") Integer cid);

    Class  getStudentByStepOne(@Param("cid") Integer cid);

}
