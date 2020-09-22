package moe.xinmu.pagehelper.test.oracle;

import lombok.Data;

import java.util.Date;

@Data
public class EMP {
    int empno;
    String ename;
    String job;
    Integer mgr;
    Date hiredate;
    float sal;
    Float comm;
    int deptno;
}
