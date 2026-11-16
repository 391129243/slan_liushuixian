package com.winters.boweilibrary.BoWei.bioweyenum;

/**
 * 运行结果枚举
 * 运行正常 0
 * 项目不存在 1
 * 串口不存在 2
 * 模块正在运行中
 * 孔对象不正确
 * 参数有误5
 * 路径有误6
 */
public enum RunExperimentResult {
    Normal(0,"运行正常"),
    ProjectNoFindError(1,"项目不存在"),
    USBNoFindError(2,"串口不存在"),
    RunningError(3,"模块正在运行中"),
    WellError(4,"孔对象不正确"),
    ParamNull(5,"运行参数空"),
    PathError(6,"路径有误");
    private int code;
    private String name;
    RunExperimentResult(int code,String name){
        this.code = code;
        this.name= name;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
