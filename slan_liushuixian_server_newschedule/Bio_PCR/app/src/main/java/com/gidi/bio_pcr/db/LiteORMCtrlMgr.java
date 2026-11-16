package com.gidi.bio_pcr.db;


import android.util.Log;

import com.gidi.bio_pcr.bean.ExQueryCondition;
import com.gidi.bio_pcr.bean.QueryCondition;
import com.gidi.bio_pcr.util.StringUtil;
import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.assit.QueryBuilder;
import com.litesuits.orm.db.assit.WhereBuilder;
import com.litesuits.orm.db.model.ColumnsValue;
import com.litesuits.orm.db.model.ConflictAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LiteORMCtrlMgr {

    private static  LiteORMCtrlMgr mInstance;

    public static LiteORMCtrlMgr getInstance(){
        if(null == mInstance){
            mInstance = new LiteORMCtrlMgr();
        }
        return  mInstance;
    }

    public LiteORMCtrlMgr(){
        LiteOrmDBUtil.getLiteOrm();
    }

    public  long insertExperiment(ExperimentModel experimentModel){
        long result =  LiteOrmDBUtil.getLiteOrm().insert(experimentModel);
        return  result;
    }

    /**
     * 1-1查询所有的实验记录
     * @return
     */
    public  List<ExperimentModel> getAllExperiment(){
        QueryBuilder<ExperimentModel> qb = new QueryBuilder<>(ExperimentModel.class)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
       // return  LiteOrmDBUtil.getQueryAll(ExperimentModel.class);
    }

    /**
     * 1-1查询第一页的实验记录
     * @return
     */
    public  List<ExperimentModel> getAllExperiment(int start,int length){

        QueryBuilder<ExperimentModel> qb1 = new QueryBuilder(ExperimentModel.class)
                .appendOrderDescBy("id")
                .limit(start, length);
        return LiteOrmDBUtil.getLiteOrm().query(qb1);
    }

    public  long getAllExperimentNumber(){
        return LiteOrmDBUtil.getLiteOrm().queryCount(ExperimentModel.class);
    }

    /**
     * 根据实验起始时间查询所有的实验个数
     * @param startTime
     * @return
     */
    public long getExperimentNumByTime(String startTime){
        QueryBuilder<ExperimentModel> qb = new QueryBuilder<ExperimentModel>(ExperimentModel.class)
                .where("StartTime LIKE ?", new Object[]{"%" + startTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * 1-3-2根据测试时间查询实验
     * @param startTime 时间
     * @param start 查询起始的位置
     * @param length 位移：需要一次查询的数量
     * @return
     */
    public List<ExperimentModel> getExperimentByTime(String startTime,int start,int length){
        QueryBuilder<ExperimentModel> qb = new QueryBuilder<ExperimentModel>(ExperimentModel.class)
                .where("StartTime LIKE ?", new Object[]{"%" + startTime+ "%"})
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    /**
     * 根据实验起始时间查询所有的实验个数
     * @param testItem 检测项目
     * @return
     */
    public long getExperimentNumByItem(String testItem){
        QueryBuilder<ExperimentModel> qb = new QueryBuilder<>(ExperimentModel.class)
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * 根据实验起始时间和检测项目查询所有的实验个数
     * @param testItem 检测项目
     * @return
     */
    public long getExperimentNumByTimeAndItem(String testItem,String startTime){
        QueryBuilder<ExperimentModel> qb = new QueryBuilder<>(ExperimentModel.class)
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .where("StartTime LIKE ?", new Object[]{"%" + startTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }


    public long getExperimentNumber(ExQueryCondition queryCondition){
        if(queryCondition != null) {
            if (StringUtil.isEmpty(queryCondition.getExStartTime())
                    && StringUtil.isEmpty(queryCondition.getExItem())) {
                return getAllExperimentNumber();
                //return getAllExperiment(0,10);//20250211
            } else if (!StringUtil.isEmpty(queryCondition.getExStartTime())
                    && StringUtil.isEmpty(queryCondition.getExItem())) {
                return getExperimentNumByTime(queryCondition.getExStartTime());

            } else if (StringUtil.isEmpty(queryCondition.getExStartTime())
                    && !StringUtil.isEmpty(queryCondition.getExItem())) {
                return getExperimentNumByItem(queryCondition.getExItem());

            } else if (!StringUtil.isEmpty(queryCondition.getExStartTime())
                    && !StringUtil.isEmpty(queryCondition.getExItem())) {
                return getExperimentNumByTimeAndItem(queryCondition.getExItem(),
                        queryCondition.getExStartTime());
            }
        }
        return 0;
    }

    /**
     * 1-2根据测试时间和项目查询实验
     */
    public List<ExperimentModel> searchExprement(ExQueryCondition queryCondition){
        if(queryCondition != null){
            if(StringUtil.isEmpty(queryCondition.getExStartTime())
                    && StringUtil.isEmpty(queryCondition.getExItem())){
                return getAllExperiment(queryCondition.getStart(),queryCondition.getLength());

            }else if(!StringUtil.isEmpty(queryCondition.getExStartTime())
                && StringUtil.isEmpty(queryCondition.getExItem())){
                return getExperimentByTime(queryCondition.getExStartTime()
                        ,queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getExStartTime())
                    && !StringUtil.isEmpty(queryCondition.getExItem())){
                return getExperimentByItem(queryCondition.getExItem(),
                        queryCondition.getStart(),queryCondition.getLength());
            }else if(!StringUtil.isEmpty(queryCondition.getExStartTime())
                    && !StringUtil.isEmpty(queryCondition.getExItem())) {
                return getExperimentByItemAndTime(queryCondition.getExItem(),
                        queryCondition.getExStartTime(),queryCondition.getStart(),queryCondition.getLength());
            }
        }
        return null;
    }


    /**
     * 1-3 删除所有的实验记录以及所有的孔位记录
     */
    public void deleteAllExperiments(){
        LiteOrmDBUtil.getLiteOrm().deleteAll(ExperimentModel.class);
       // deleteAllSLANWellResult();
    }

    /**
     *  喊出删除宏石类型的实验
     */

    public void deleteAllSLANExperiment(){
        WhereBuilder del_slan_ex_builder = new WhereBuilder(ExperimentModel.class)
                .equals(DatabaseHelper.COLUMN_EX_TYPE,2);
        LiteOrmDBUtil.getLiteOrm().delete(del_slan_ex_builder);
    }



    /**
     * 删除选中实验
     * @param selectExperiments
     */
    public int deleteExperiments(List<ExperimentModel> selectExperiments){
        int result = -1;
        for(ExperimentModel experimentModel : selectExperiments){
            String startTime = experimentModel.getStartTime();
            String testItem = experimentModel.getTestItem();
            int exptype = experimentModel.getExpType();//实验类型，丁智是1，宏石是2
            int del_ex_result =  LiteOrmDBUtil.getLiteOrm().delete(new WhereBuilder(ExperimentModel.class)
                            .equals(DatabaseHelper.COLUMN_TEST_ITEM,testItem)
                            .and()
                            .where(DatabaseHelper.COLUMN_EX_STARTTIME + " LIKE ?",  new Object[]{"%" + startTime+ "%"}));

            Log.i("LiteORMCtrlMgr","数据库删除实验记录个数"+del_ex_result);
            if (del_ex_result != -1){
                result++;
            }
             if(exptype == 2){
                WhereBuilder slan_del = new WhereBuilder(SLANWellResult.class)
                        .equals(DatabaseHelper.COLUMN_TEST_ITEM,testItem)
                        .and()
                        .where("startTime LIKE ?", new Object[]{"%" + startTime+ "%"});
                int del_slan_well = LiteOrmDBUtil.getLiteOrm().delete(slan_del);
                Log.i("LiteORMCtrlMgr","删除宏石的孔位数据个数"+del_slan_well);
            }

        }
        return result;
    }

    /**
     * 1-3根据测试时间查询实验
     * @param startTime 时间
     * @return
     */
    public List<ExperimentModel> getExperimentByTime(String startTime){
        QueryBuilder<ExperimentModel> qb = new QueryBuilder<ExperimentModel>(ExperimentModel.class)
                .where("StartTime LIKE ?", new Object[]{"%" + startTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    /**
     * 1-4根据测试项目查询实验
     * @param testItem 实验项目
     * @return
     */
    public List<ExperimentModel> getExperimentByItem(String testItem){
        QueryBuilder<ExperimentModel> qb = new QueryBuilder<>(ExperimentModel.class)
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    /**
     * 1-4-2根据测试项目查询实验
     * @param testItem 实验项目
     * @return
     */
    public List<ExperimentModel> getExperimentByItem(String testItem,int start,int length){
        QueryBuilder<ExperimentModel> qb = new QueryBuilder<>(ExperimentModel.class)
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

        public List<ExperimentModel> getExperimentByItemAndTime(String testItem,String startTime){

            QueryBuilder<ExperimentModel> qb = new QueryBuilder<>(ExperimentModel.class)
                    .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                     .where("StartTime LIKE ?", new Object[]{"%" + startTime+ "%"})
                    .appendOrderDescBy("id");
            return LiteOrmDBUtil.getLiteOrm().query(qb);
        }
    /**
     * 1-5-2根据测试项目与起始时间共同查询实验
     * @param testItem 实验项目
     * @return
     */
    public List<ExperimentModel> getExperimentByItemAndTime(String testItem,String startTime,int start,int length){

        QueryBuilder<ExperimentModel> qb = new QueryBuilder<>(ExperimentModel.class)
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .where("StartTime LIKE ?", new Object[]{"%" + startTime+ "%"})
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    /***************************************更新二次分析的数据库*****************************************************/


/*********************************************博维**********************************************************/

    /**
     * Bioway_WELL01-2-01
     * 条件1
     * 0000
     * 所有查询条件均空，可以查询所有的孔位结果个数
     */
    public long getAllBiowayWellResultNum(){
        return LiteOrmDBUtil.getLiteOrm().queryCount(BioWayWellResult.class);
    }


    /**
     * Bioway_WELL_01-2-02
     * 条件1
     * 0001
     * 所有查询条件为项目，可以查询所有的孔位结果个数
     */
    public long getAllBiowayWellNumByItem(String testItem){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }


    /**
     * Bioway_WELL_01-2-03
     * 条件1
     * 0010
     * 所有查询条件为检测开始时间，可以查询所有的孔位结果个数
     */
    public long getBiowayWellNumByTime(String startTime){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<BioWayWellResult>(BioWayWellResult.class)
                .where("startTime LIKE ?", new Object[]{"%" + startTime+ "%"})
                .distinct(false)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-04
     * 条件4
     * 0011
     * 所有查询条件为检测开始时间余检测项目，可以查询所有的孔位结果个数
     */
    public long getBiowayWellNumByTimeAndItem(String testItem,String testtime){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{testItem})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testtime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-05
     * 条件5
     * 0100
     * 所有查询条件为检测开始时间余检测项目，可以查询所有的孔位结果个数
     */

    private long getBiowayWellNumByRegentCode(String regentCode){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-06
     * 条件6
     * 0101
     * 所有查询条件为试剂条码与检测项目，可以查询所有的孔位结果个数
     */
    private long getBiowayWellNumByRegentCodeAndItem(String regentCode,String testItem){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * Bioway_WELL_01-2-07
     * 条件7 0110
     * 条码和时间查询孔位记录个数
     * @param regentCode
     * @param time
     * @return
     */
    //0110(wan)
    private long getBiowayWellNumByTimeAndRegentCode(String regentCode,String time){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + time+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * Bioway_WELL_01-2-08
     *   条件8 0111
     * 条码，时间和项目查询博维孔位记录个数
     * @param regentCode
     * @param time
     * @param testItem
     * @return
     */
    //0111(wan)
    private long getBiowayWellNumByTimeAndCodeAndItem(String regentCode,String time,String testItem){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?",new Object[]{testItem})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + time+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * Bioway_WELL_01-2-09
     *  条件9 1000
     * 身份证查询孔位个数
     * @param idcard 身份证
     * @return
     */
    private long getBiowayWellNumByID(String idcard){
        Log.i("LiteORMCtrlMgr", "查询条件" + "getByID");
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * Bioway_WELL_01-2-10
     * 条件10 1001
     * 身份证和测试项目共同查询
     * @param idcard
     * @param testItem
     * @return
     */
    //1001(wan)
    private long getBiowayWellNumByIDAndItem(String idcard,String testItem){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * Bioway_WELL_01-2-11
     * 条件11 1010
     * 身份证和测试时间1010
     * @param idcard
     * @param testTime
     * @return
     */
    private long getBiowayWellNumByIDAndTime(String idcard,String testTime){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * Bioway_WELL_01-2-12
     *      * 条件12 1011
     * 身份证&测试时间&测试项目
     * @param idcard
     * @param testTime
     * @param testItem
     * @return
     */
    private long getBiowayWellNumByIDAndTimeAndItem(String idcard,String testTime,String testItem){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{testItem})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * Bioway_WELL_01-2-13
     *      * 条件13 1100
     * 身份证&试剂编码
     * @param idcard
     * @param regentCode 试剂编码
     * @return
     */
    //1100(wan)
    private long getBiowayWellNumByIDAndCode(String idcard,String regentCode){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * Bioway_WELL_01-2-14
     * 条件14 1101
     * 身份证，试剂卡，测试项目
     * @param idcard
     * @param regentCode
     * @param testItem
     * @return
     */
    //1101
    private long getBiowayWellNumByIDAndCodeAndItem(String idcard,String regentCode,String testItem){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }


    /**
     * Bioway_WELL_01-2-15
     * 条件15 1110
     * 身份证，试剂卡，测试时间
     * @param idcard
     * @param regentCode
     * @param testTime
     * @return
     */
    private long getBiowayWellNumByIDAndCodeAndTime(String idcard,String regentCode,String testTime){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * Bioway_WELL_01-2-16
     * 条件16 1110
     * 身份证，试剂卡，测试时间，测试项目
     * @return
     */
    private long getBiowayWellNumByAll(QueryCondition queryCondition){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{queryCondition.getRegentCode()})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{queryCondition.getIdcardNum()})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{queryCondition.getTestItem()})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + queryCondition.getTestStartTime()+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2
     * 根据条件获取博维所有的孔位结果个数
     * @return
     */
    public long getBiowayWellResultNum(QueryCondition queryCondition){
        if(null != queryCondition){
            if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0000
                return getAllBiowayWellResultNum();
            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0001
                return getAllBiowayWellNumByItem(queryCondition.getTestItem());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0010
                return getBiowayWellNumByTime(queryCondition.getTestStartTime());

            }else if (StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0011测试身份证和试剂卡
                return getBiowayWellNumByTimeAndItem(queryCondition.getTestItem(), queryCondition.getTestStartTime());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0100
                return getBiowayWellNumByRegentCode(queryCondition.getRegentCode());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0101
                return getBiowayWellNumByRegentCodeAndItem(queryCondition.getRegentCode(),queryCondition.getTestItem());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0110
                return getBiowayWellNumByTimeAndRegentCode(queryCondition.getRegentCode(),queryCondition.getTestStartTime());
            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0111
                return getBiowayWellNumByTimeAndCodeAndItem(queryCondition.getRegentCode(),queryCondition.getTestStartTime(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1000
                return getBiowayWellNumByID(queryCondition.getIdcardNum());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1001
                return getBiowayWellNumByIDAndItem(queryCondition.getIdcardNum(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1010
                return getBiowayWellNumByIDAndTime(queryCondition.getIdcardNum(),queryCondition.getTestStartTime());

            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1011
                return getBiowayWellNumByIDAndTimeAndItem(queryCondition.getIdcardNum(),queryCondition.getTestStartTime(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1100
                return getBiowayWellNumByIDAndCode(queryCondition.getIdcardNum(),queryCondition.getRegentCode());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1101
                return getBiowayWellNumByIDAndCodeAndItem(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1110
                return getBiowayWellNumByIDAndCodeAndTime(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),queryCondition.getTestStartTime());
            }else{
                return getBiowayWellNumByAll(queryCondition);
            }
        }
        return 0;
    }



    /**
     * 插入博维的孔位信息
     */
    public long insertBiowayWellResult(BioWayWellResult biowayWellResult){
        long result =  LiteOrmDBUtil.getLiteOrm().insert(biowayWellResult);
        return  result;
    }

    public List<BioWayWellResult> searchBiowayWellResult(QueryCondition queryCondition){
        if(queryCondition != null){
            if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0000
                return getAllBiowayWellResult();
            }else if (StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0011测试身份证和试剂卡
                return getBiowayExperimentWell(queryCondition.getTestItem(), queryCondition.getTestStartTime());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0001
                return getBiowayWellResultByItem(queryCondition.getTestItem());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0010
                return getBiowayWellResultFromTime(queryCondition.getTestStartTime());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0100
                return getBiowayWellResultFromRegentCode(queryCondition.getRegentCode());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0101
                return getBiowayByRegentCodeAndTestItem(queryCondition.getRegentCode(),queryCondition.getTestItem());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0110
                return getBiowayByTimeAndRegentCode(queryCondition.getRegentCode(),queryCondition.getTestStartTime());
            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0111
                return getBiowayByTimeAndCodeAndItem(queryCondition.getRegentCode(),queryCondition.getTestStartTime(), queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1000
                return getBiowayByID(queryCondition.getIdcardNum());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1001
                return getBiowayByIDAndItem(queryCondition.getIdcardNum(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1010
                return getBiowayByIDAndTime(queryCondition.getIdcardNum(),queryCondition.getTestStartTime());

            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1011
                return getBiowayByIDAndTimeAndItem(queryCondition.getIdcardNum(),queryCondition.getTestStartTime(), queryCondition.getTestItem());
            } else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1100
                return getBiowayByIDAndRegentCode(queryCondition.getIdcardNum(),queryCondition.getRegentCode());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1101
                return getBiowayByIDAndCodeAndItem(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),
                        queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1110
                return getBiowayByIDAndCodeAndTime(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),
                        queryCondition.getTestStartTime(),queryCondition.getStart(),queryCondition.getLength());
            }else{
                return getBiowayByAll(queryCondition);
            }


        }
        return  null;

    }

    public List<BioWayWellResult> getAllBiowayWellResult(){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    public List<BioWayWellResult> getAllBiowayWellResult(int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .appendOrderDescBy("id").limit(start, length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    public List<BioWayWellResult> searchBiowayWellResult(QueryCondition queryCondition,int start,int length){
        if(queryCondition != null){
            if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0000
                return getAllBiowayWellResult(queryCondition.getStart(),queryCondition.getLength());
            }else if (StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0011测试身份证和试剂卡
                return getBiowayExperimentWell(queryCondition.getTestItem(), queryCondition.getTestStartTime()
                        ,queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0001
                return getBiowayWellResultByItem(queryCondition.getTestItem(),queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0010
                return getBiowayWellResultFromTime(queryCondition.getTestStartTime(),queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0100
                return getBiowayWellResultFromRegentCode(queryCondition.getRegentCode(),queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0101
                return getBiowayByRegentCodeAndTestItem(queryCondition.getRegentCode(),queryCondition.getTestItem()
                        ,queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0110
                return getBiowayByTimeAndRegentCode(queryCondition.getRegentCode(),queryCondition.getTestStartTime()
                        ,queryCondition.getStart(),queryCondition.getLength());
            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0111
                return getBiowayByTimeAndCodeAndItem(queryCondition.getRegentCode(),queryCondition.getTestStartTime(),
                        queryCondition.getTestItem(),queryCondition.getStart(),queryCondition.getLength());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1000
                return getBiowayByID(queryCondition.getIdcardNum(),queryCondition.getStart(),queryCondition.getLength());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1001
                return getBiowayByIDAndItem(queryCondition.getIdcardNum(),queryCondition.getTestItem()
                        ,queryCondition.getStart(),queryCondition.getLength());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1010
                return getBiowayByIDAndTime(queryCondition.getIdcardNum(),queryCondition.getTestStartTime()
                        ,queryCondition.getStart(),queryCondition.getLength());

            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1011
                return getBiowayByIDAndTimeAndItem(queryCondition.getIdcardNum(),queryCondition.getTestStartTime(),
                        queryCondition.getTestItem(),queryCondition.getStart(),queryCondition.getLength());
            } else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1100
                return getBiowayByIDAndRegentCode(queryCondition.getIdcardNum(),queryCondition.getRegentCode()
                        ,queryCondition.getStart(),queryCondition.getLength());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1101
                return getBiowayByIDAndCodeAndItem(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),
                        queryCondition.getTestItem(),queryCondition.getStart(),queryCondition.getLength());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1110
                return getBiowayByIDAndCodeAndTime(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),
                        queryCondition.getTestStartTime(),queryCondition.getStart(),queryCondition.getLength());
            }else{
                return getBiowayByAll(queryCondition,queryCondition.getStart(),queryCondition.getLength());
            }


        }
        return  null;

    }

    public List<BioWayWellResult> getBiowayExperimentWell(String testItem,String testtime){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{testItem})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testtime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    //0001
    private List<BioWayWellResult> getBiowayWellResultByItem(String testItem){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    public List<BioWayWellResult> getBiowayWellResultFromTime(String searchTime){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where("startTime LIKE ?", new Object[]{"%" + searchTime+ "%"})
                .distinct(false)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    //0100(WAN)
    private List<BioWayWellResult> getBiowayWellResultFromRegentCode(String regentCode){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }


    //0101(wan)
    private List<BioWayWellResult>getBiowayByRegentCodeAndTestItem(String regentCode,String testItem){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    /**
     * 条码和时间查询
     * @param regentCode
     * @param time
     * @return
     */
    //0110(wan)
    private List<BioWayWellResult> getBiowayByTimeAndRegentCode(String regentCode,String time){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + time+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    /**
     * 条码，时间和项目
     * @param regentCode
     * @param time
     * @param testItem
     * @return
     */
    //0111(wan)
    private List<BioWayWellResult> getBiowayByTimeAndCodeAndItem(String regentCode,String time,String testItem){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?",new Object[]{testItem})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + time+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    //1000(wan)
    private List<BioWayWellResult> getBiowayByID(String idcard){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    /**
     * 身份证和测试项目共同查询
     * @param idcard
     * @param testItem
     * @return
     */
    //1001(wan)
    private List<BioWayWellResult> getBiowayByIDAndItem(String idcard,String testItem){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    /**
     * 身份证和测试时间1001
     * @param idcard
     * @param testTime
     * @return
     */
    private List<BioWayWellResult> getBiowayByIDAndTime(String idcard,String testTime){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    /**
     * 身份证&测试时间&测试项目
     * @param idcard
     * @param testTime
     * @param testItem
     * @return
     */
    private List<BioWayWellResult> getBiowayByIDAndTimeAndItem(String idcard,String testTime,String testItem){

        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{testItem})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    //1100(wan)
    private List<BioWayWellResult> getBiowayByIDAndRegentCode(String idcard,String regentCode){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    /**
     * 身份证，试剂卡，测试项目
     * @param idcard
     * @param regentCode
     * @param testItem
     * @return
     */
    //1101
    private List<BioWayWellResult> getBiowayByIDAndCodeAndItem(String idcard,String regentCode,String testItem){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<BioWayWellResult> getBiowayByIDAndCodeAndTime(String idcard,String regentCode,String testTime){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<BioWayWellResult> getBiowayByAll(QueryCondition queryCondition){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{queryCondition.getRegentCode()})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{queryCondition.getIdcardNum()})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{queryCondition.getTestItem()})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + queryCondition.getTestStartTime()+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    public List<BioWayWellResult> getBiowayExperimentWell(String testItem,String testtime,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{testItem})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testtime+ "%"})
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    //0001
    private List<BioWayWellResult> getBiowayWellResultByItem(String testItem,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    public List<BioWayWellResult> getBiowayWellResultFromTime(String searchTime,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where("startTime LIKE ?", new Object[]{"%" + searchTime+ "%"})
                .distinct(false)
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    //0100(WAN)
    private List<BioWayWellResult> getBiowayWellResultFromRegentCode(String regentCode,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }


    //0101(wan)
    private List<BioWayWellResult>getBiowayByRegentCodeAndTestItem(String regentCode,String testItem,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    /**
     * 条码和时间查询
     * @param regentCode
     * @param time
     * @return
     */
    //0110(wan)
    private List<BioWayWellResult> getBiowayByTimeAndRegentCode(String regentCode,String time,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + time+ "%"})
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    /**
     * 条码，时间和项目
     * @param regentCode
     * @param time
     * @param testItem
     * @return
     */
    //0111(wan)
    private List<BioWayWellResult> getBiowayByTimeAndCodeAndItem(String regentCode,String time,String testItem,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?",new Object[]{testItem})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + time+ "%"})
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    //1000(wan)
    private List<BioWayWellResult> getBiowayByID(String idcard,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    /**
     * 身份证和测试项目共同查询
     * @param idcard
     * @param testItem
     * @return
     */
    //1001(wan)
    private List<BioWayWellResult> getBiowayByIDAndItem(String idcard,String testItem,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    /**
     * 身份证和测试时间1001
     * @param idcard
     * @param testTime
     * @return
     */
    private List<BioWayWellResult> getBiowayByIDAndTime(String idcard,String testTime,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    /**
     * 身份证&测试时间&测试项目
     * @param idcard
     * @param testTime
     * @param testItem
     * @return
     */
    private List<BioWayWellResult> getBiowayByIDAndTimeAndItem(String idcard,String testTime,String testItem,int start,int length){

        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{testItem})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    //1100(wan)
    private List<BioWayWellResult> getBiowayByIDAndRegentCode(String idcard,String regentCode,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    /**
     * 身份证，试剂卡，测试项目
     * @param idcard
     * @param regentCode
     * @param testItem
     * @return
     */
    //1101
    private List<BioWayWellResult> getBiowayByIDAndCodeAndItem(String idcard,String regentCode,String testItem,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<BioWayWellResult> getBiowayByIDAndCodeAndTime(String idcard,String regentCode,String testTime,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<BioWayWellResult> getBiowayByAll(QueryCondition queryCondition,int start,int length){
        QueryBuilder<BioWayWellResult> qb = new QueryBuilder<>(BioWayWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{queryCondition.getRegentCode()})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{queryCondition.getIdcardNum()})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{queryCondition.getTestItem()})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + queryCondition.getTestStartTime()+ "%"})
                .appendOrderDescBy("id").limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    /**
     * 删除所有宏石的孔位信息
     */
    public void deleteAllBiowayWellResult(){
        LiteOrmDBUtil.getLiteOrm().deleteAll(BioWayWellResult.class);
    }


    /**
     * 删除宏石的孔位信息
     */
    public int delBiowayWellResult(List<BioWayWellResult> deleteWellResuts){
        int index = 0;
        for(BioWayWellResult wellResult :deleteWellResuts){
            String startTime = wellResult.getStartTime();
            String testItem = wellResult.getTestItem();
            String reagentCode = wellResult.getReagentCode();
            int del_slan_result =  LiteOrmDBUtil.getLiteOrm().delete(new WhereBuilder(BioWayWellResult.class)
                    .where("startTime LIKE ?", new Object[]{"%" + startTime+ "%"})
                    .and()
                    .equals(DatabaseHelper.COLUMN_TEST_ITEM,testItem)
                    .and()
                    .equals(DatabaseHelper.COLUMN_REGENTCODE,reagentCode));
            index += del_slan_result;

        }
        Log.i("AABBCC","--删除宏石孔位个数"+ index);
        return index;
    }


    /**
     * 更新宏石的孔位信息
     * @param slanWellResult
     * @param updateMap
     * @return
     */

    public int updateBiowayWellResult(BioWayWellResult slanWellResult, HashMap<String, Object> updateMap){
        return LiteOrmDBUtil.getLiteOrm().
                update(slanWellResult, new ColumnsValue(updateMap), ConflictAlgorithm.Fail);
    }

/*****************************************************宏石******************************************************/
    /**
     * 插入宏石的孔位信息
     */
    public long insertSLANWellResult(SLANWellResult slanWellResult){
        long result =  LiteOrmDBUtil.getLiteOrm().insert(slanWellResult);
        return  result;
    }
    public List<SLANWellResult> getAllSLANWellResult(){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    public List<SLANWellResult> getAllSLANWellResult(int start,int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    public long getAllSLANWellResultNumbers(){

        return LiteOrmDBUtil.getLiteOrm().queryCount(SLANWellResult.class);
    }

    public List<SLANWellResult> getSLANExperimentWell(String testItem,String testtime){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{testItem})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testtime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    public List<SLANWellResult> getSLANExperimentWell(String testItem,String testtime,int start,int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new String[]{testItem})
                .whereAnd("startTime LIKE ?", new String[]{"%" + testtime+ "%"})
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    //0001
    private List<SLANWellResult> getSLANWellResultByItem(String testItem){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult> getSLANWellResultByItem(String testItem,int start,int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }


    public List<SLANWellResult> getSLANWellResultFromTime(String searchTime){

        QueryBuilder<SLANWellResult> qb = new QueryBuilder<SLANWellResult>(SLANWellResult.class)
                .where("startTime LIKE ?", new Object[]{"%" + searchTime+ "%"})
                .distinct(false)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    public List<SLANWellResult> getSLANWellResultFromTime(String searchTime,int start,int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<SLANWellResult>(SLANWellResult.class)
                .where("startTime LIKE ?", new String[]{"%" + searchTime+ "%"})
                .distinct(false)
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }


    //0100(WAN)
    private List<SLANWellResult> getSLANWellResultFromRegentCode(String regentCode){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult> getSLANWellResultFromRegentCode(String regentCode,int start,int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }



    //0101(wan)
    private List<SLANWellResult>getSLANByRegentCodeAndTestItem(String regentCode,String testItem){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult>getSLANByRegentCodeAndTestItem(String regentCode,String testItem,int start,int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }


    /**
     * 条码和时间查询
     * @param regentCode
     * @param time
     * @return
     */
    //0110(wan)
    private List<SLANWellResult> getSLANByTimeAndRegentCode(String regentCode,String time){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + time+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);

    }
    private List<SLANWellResult> getSLANByTimeAndRegentCode(String regentCode,String time,int start,int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new String[]{regentCode})
                .whereAnd("startTime LIKE ?", new String[]{"%" + time+ "%"})
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }




    /**
     * 条码，时间和项目
     * @param regentCode
     * @param time
     * @param testItem
     * @return
     */
    //0111(wan)
    private List<SLANWellResult> getSLANByTimeAndRegentCodeAndItem(String regentCode,String time,String testItem){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?",new Object[]{testItem})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + time+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult> getSLANByTimeAndRegentCodeAndItem(String regentCode,
                                                                   String time,
                                                                   String testItem,
                                                                   int start,
                                                                   int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new String[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?",new String[]{testItem})
                .whereAnd("startTime LIKE ?", new String[]{"%" + time+ "%"})
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    //1000(wan)
    private List<SLANWellResult> getSLANByID(String idcard){
        Log.i("abcde", "查询条件" + "getByID");
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult> getSLANByID(String idcard,
                                             int start,
                                             int length){
        Log.i("abcde", "查询条件" + "getByID");
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }



    /**
     * 身份证和测试项目共同查询
     * @param idcard
     * @param testItem
     * @return
     */
    //1001(wan)
    private List<SLANWellResult> getSLANByIDAndItem(String idcard,String testItem){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult> getSLANByIDAndItem(String idcard,
                                                    String testItem,
                                                    int start,
                                                    int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    /**
     * 身份证和测试时间1001
     * @param idcard
     * @param testTime
     * @return
     */
    private List<SLANWellResult> getSLANByIDAndTime(String idcard,String testTime){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult> getSLANByIDAndTime(String idcard,
                                                    String testTime,
                                                    int start,
                                                    int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new String[]{idcard})
                .whereAnd("startTime LIKE ?", new String[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }

    /**
     * 身份证&测试时间&测试项目
     * @param idcard
     * @param testTime
     * @param testItem
     * @return
     */
    private List<SLANWellResult> getSLANByIDAndTimeAndItem(String idcard,String testTime,String testItem){

        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{testItem})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult> getSLANByIDAndTimeAndItem(String idcard,
                                                           String testTime,
                                                           String testItem,
                                                           int start,
                                                           int length){

        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new String[]{testItem})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new String[]{idcard})
                .whereAnd("startTime LIKE ?", new String[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }


    //1100(wan)
    private List<SLANWellResult> getSLANByIDAndRegentCode(String idcard,String regentCode){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult> getSLANByIDAndRegentCode(String idcard,
                                                          String regentCode,
                                                          int start,
                                                          int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }


    /**
     * 身份证，试剂卡，测试项目
     * @param idcard
     * @param regentCode
     * @param testItem
     * @return
     */
    //1101
    private List<SLANWellResult> getSLANByIDAndRegentCodeAndItem(String idcard,String regentCode,String testItem){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult> getSLANByIDAndRegentCodeAndItem(String idcard,
                                                                 String regentCode,
                                                                 String testItem,
                                                                 int start,
                                                                 int length) {
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id")
                . limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }


    private List<SLANWellResult> getSLANByIDAndRegentCodeAndTime(String idcard,String regentCode,String testTime){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{idcard})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult> getSLANByIDAndRegentCodeAndTime(String idcard,
                                                                 String regentCode,
                                                                 String testTime,
                                                                 int start,
                                                                 int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new String[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new String[]{idcard})
                .whereAnd("startTime LIKE ?", new String[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }


    private List<SLANWellResult> getSLANByAll(QueryCondition queryCondition){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new Object[]{queryCondition.getRegentCode()})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new Object[]{queryCondition.getIdcardNum()})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new Object[]{queryCondition.getTestItem()})
                .whereAnd("startTime LIKE ?", new Object[]{"%" + queryCondition.getTestStartTime()+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }
    private List<SLANWellResult> getSLANByAll(QueryCondition queryCondition,int start,int length){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new String[]{queryCondition.getRegentCode()})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new String[]{queryCondition.getIdcardNum()})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new String[]{queryCondition.getTestItem()})
                .whereAnd("startTime LIKE ?", new String[]{"%" + queryCondition.getTestStartTime()+ "%"})
                .appendOrderDescBy("id")
                .limit(start,length);
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }


    public List<SLANWellResult> searchSLANWellResult(QueryCondition queryCondition){
        if(queryCondition != null){
            if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0000
                return getAllSLANWellResult();
            }else if (StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0011测试身份证和试剂卡
                return getSLANExperimentWell(queryCondition.getTestItem(), queryCondition.getTestStartTime());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0001
                return getSLANWellResultByItem(queryCondition.getTestItem());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0010
                return getSLANWellResultFromTime(queryCondition.getTestStartTime());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0100
                return getSLANWellResultFromRegentCode(queryCondition.getRegentCode());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0101
                return getSLANByRegentCodeAndTestItem(queryCondition.getRegentCode(),queryCondition.getTestItem());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0110
                return getSLANByTimeAndRegentCode(queryCondition.getRegentCode(),queryCondition.getTestStartTime());
            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0111
                return getSLANByTimeAndRegentCodeAndItem(queryCondition.getRegentCode(),queryCondition.getTestStartTime(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1000
                return getSLANByID(queryCondition.getIdcardNum());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1001
                return getSLANByIDAndItem(queryCondition.getIdcardNum(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1010
                return getSLANByIDAndTime(queryCondition.getIdcardNum(),queryCondition.getTestStartTime());

            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1011
                return getSLANByIDAndTimeAndItem(queryCondition.getIdcardNum(),queryCondition.getTestStartTime(),queryCondition.getTestItem());
            } else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1100
                return getSLANByIDAndRegentCode(queryCondition.getIdcardNum(),queryCondition.getRegentCode());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1101
                return getSLANByIDAndRegentCodeAndItem(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1110
                return getSLANByIDAndRegentCodeAndTime(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),queryCondition.getTestStartTime());
            }else{
                return getSLANByAll(queryCondition);
            }


        }
        return  null;

    }


    /**
     * 删除宏石的孔位信息
     */
    public int deleteSLANWellResult(List<SLANWellResult> deleteWellResuts){
        int index = 0;
        for(SLANWellResult slanWellResult :deleteWellResuts){
            String startTime = slanWellResult.getStartTime();
            String testItem = slanWellResult.getTestItem();
            String reagentCode = slanWellResult.getReagentCode();
            int del_slan_result =  LiteOrmDBUtil.getLiteOrm().delete(new WhereBuilder(SLANWellResult.class)
                    .where("startTime LIKE ?", new Object[]{"%" + startTime+ "%"})
                    .and()
                    .equals(DatabaseHelper.COLUMN_TEST_ITEM,testItem)
                    .and()
                    .equals(DatabaseHelper.COLUMN_REGENTCODE,reagentCode));
            index += del_slan_result;

        }
        Log.i("AABBCC","--删除宏石孔位个数"+ index);
        return index;
    }

    /**
     * 删除所有宏石的孔位信息
     */
    public void deleteAllSLANWellResult(){
        LiteOrmDBUtil.getLiteOrm().deleteAll(SLANWellResult.class);
    }
    /**
     * 更新宏石的孔位信息
     * @param slanWellResult
     * @param updateMap
     * @return
     */

    public int updateSLANWellResult(SLANWellResult slanWellResult, HashMap<String, Object> updateMap){
        return LiteOrmDBUtil.getLiteOrm().
                update(slanWellResult, new ColumnsValue(updateMap), ConflictAlgorithm.Fail);
    }

    ///////登录账户////////
    /**
     * 插入登录账户
     */
    public  long insertLoginUser(LoginUser user){
        long result =  LiteOrmDBUtil.getLiteOrm().insert(user);
        return  result;
    }

    /**
     * 删除登录账户
     */
    public int deleteLoginUser(LoginUser user){
        WhereBuilder user_delete = new WhereBuilder(LoginUser.class)
                .equals(DatabaseHelper.COLUMN_USER_ACCOUNT,user.getUserAccount())
                .and()
                .equals(DatabaseHelper.COLUMN_USER_PASSWORD,user.getUserPassword());
        int del_result = LiteOrmDBUtil.getLiteOrm().delete(user_delete);
        return del_result;
    }

    /**
     * 更新登录账户
     */
    public int updateLoginUser(LoginUser user, HashMap<String, Object> updateMap){
        return LiteOrmDBUtil.getLiteOrm().
                update(user, new ColumnsValue(updateMap), ConflictAlgorithm.Fail);
    }

    /**
     * 查询所有的登录账户信息
     */
    public  List<LoginUser> getAllLoginUser(){
        QueryBuilder<LoginUser> qb = new QueryBuilder<>(LoginUser.class)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().query(qb);
    }


    //===============================宏石操作========================================//
    //获取孔位结果数量
    /**
     * SLAN_WELL_01-2-01
     * 条件1
     * 0000
     * 所有查询条件均空，可以查询所有的孔位结果个数
     */
    public long getAllSLANWellResultNum(){
        return LiteOrmDBUtil.getLiteOrm().queryCount(SLANWellResult.class);
    }

    /**
     * SLAN_WELL_01-2-02
     * 条件1
     * 0001
     * 所有查询条件为项目，可以查询所有的孔位结果个数
     */
    public long getAllSLANWellNumByItem(String testItem){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-03
     * 条件1
     * 0010
     * 所有查询条件为检测开始时间，可以查询所有的孔位结果个数
     */
    public long getSLANWellNumByTime(String startTime){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<SLANWellResult>(SLANWellResult.class)
                .where("startTime LIKE ?", new String[]{"%" + startTime+ "%"})
                .distinct(false)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-04
     * 条件4
     * 0011
     * 所有查询条件为检测开始时间余检测项目，可以查询所有的孔位结果个数
     */
    public long getSLANWellNumByTimeAndItem(String testItem,String testtime){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new String[]{testItem})
                .whereAnd("startTime LIKE ?", new String[]{"%" + testtime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-05
     * 条件5
     * 0100
     * 所有查询条件为检测开始时间余检测项目，可以查询所有的孔位结果个数
     */

    private long getSLANWellNumByRegentCode(String regentCode){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-06
     * 条件6
     * 0101
     * 所有查询条件为试剂条码与检测项目，可以查询所有的孔位结果个数
     */
    private long getSLANWellNumByRegentCodeAndItem(String regentCode,String testItem){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-07
     * 条件7 0110
     * 条码和时间查询
     * @param regentCode
     * @param time
     * @return
     */
    //0110(wan)
    private long getSLANWellNumByTimeAndRegentCode(String regentCode,String time){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new String[]{regentCode})
                .whereAnd("startTime LIKE ?", new String[]{"%" + time+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-08
     *   条件8 0111
     * 条码，时间和项目
     * @param regentCode
     * @param time
     * @param testItem
     * @return
     */
    //0111(wan)
    private long getSLANWellNumByTimeAndCodeAndItem(String regentCode,String time,String testItem){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new String[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?",new String[]{testItem})
                .whereAnd("startTime LIKE ?", new String[]{"%" + time+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-09
     *  条件9 1000
     * 身份证查询
     * @param idcard 身份证
     * @return
     */
    private long getSLANWellNumByID(String idcard){
        Log.i("LiteORMCtrlMgr", "查询条件" + "getByID");
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-10
     * 条件10 1001
     * 身份证和测试项目共同查询
     * @param idcard
     * @param testItem
     * @return
     */
    //1001(wan)
    private long getSLANWellNumByIDAndItem(String idcard,String testItem){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-11
     * 条件11 1010
     * 身份证和测试时间1010
     * @param idcard
     * @param testTime
     * @return
     */
    private long getSLANWellNumByIDAndTime(String idcard,String testTime){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new String[]{idcard})
                .whereAnd("startTime LIKE ?", new String[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-12
     *      * 条件12 1011
     * 身份证&测试时间&测试项目
     * @param idcard
     * @param testTime
     * @param testItem
     * @return
     */
    private long getSLANWellNumByIDAndTimeAndItem(String idcard,String testTime,String testItem){

        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new String[]{testItem})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new String[]{idcard})
                .whereAnd("startTime LIKE ?", new String[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-13
     *      * 条件13 1100
     * 身份证&试剂编码
     * @param idcard
     * @param regentCode 试剂编码
     * @return
     */
    //1100(wan)
    private long getSLANWellNumByIDAndCode(String idcard,String regentCode){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-14
     * 条件14 1101
     * 身份证，试剂卡，测试项目
     * @param idcard
     * @param regentCode
     * @param testItem
     * @return
     */
    //1101
    private long getSLANWellNumByIDAndCodeAndItem(String idcard,String regentCode,String testItem){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .whereEquals(DatabaseHelper.COLUMN_SAMPLENAME ,idcard)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_REGENTCODE ,regentCode)
                .whereAppendAnd()
                .whereEquals(DatabaseHelper.COLUMN_TEST_ITEM ,testItem)
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-15
     * 条件15 1110
     * 身份证，试剂卡，测试时间
     * @param idcard
     * @param regentCode
     * @param testTime
     * @return
     */
    private long getSLANWellNumByIDAndCodeAndTime(String idcard,String regentCode,String testTime){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new String[]{regentCode})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new String[]{idcard})
                .whereAnd("startTime LIKE ?", new String[]{"%" + testTime+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    /**
     * SLAN_WELL_01-2-16
     * 条件16 1110
     * 身份证，试剂卡，测试时间，测试项目
     * @return
     */
    private long getSLANWellNumByAll(QueryCondition queryCondition){
        QueryBuilder<SLANWellResult> qb = new QueryBuilder<>(SLANWellResult.class)
                .where(DatabaseHelper.COLUMN_REGENTCODE+"=?", new String[]{queryCondition.getRegentCode()})
                .whereAnd(DatabaseHelper.COLUMN_SAMPLENAME+"=?", new String[]{queryCondition.getIdcardNum()})
                .whereAnd(DatabaseHelper.COLUMN_TEST_ITEM+"=?", new String[]{queryCondition.getTestItem()})
                .whereAnd("startTime LIKE ?", new String[]{"%" + queryCondition.getTestStartTime()+ "%"})
                .appendOrderDescBy("id");
        return LiteOrmDBUtil.getLiteOrm().queryCount(qb);
    }

    public long getSLANWellResultCount(QueryCondition queryCondition){
        if(null != queryCondition){
            if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0000
                return getAllSLANWellResultNum();
            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0001
                return getAllSLANWellNumByItem(queryCondition.getTestItem());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0010
                return getSLANWellNumByTime(queryCondition.getTestStartTime());

            }else if (StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0011测试身份证和试剂卡
                return getSLANWellNumByTimeAndItem(queryCondition.getTestItem(), queryCondition.getTestStartTime());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0100
                return getSLANWellNumByRegentCode(queryCondition.getRegentCode());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0101
                return getSLANWellNumByRegentCodeAndItem(queryCondition.getRegentCode(),queryCondition.getTestItem());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0110
                return getSLANWellNumByTimeAndRegentCode(queryCondition.getRegentCode(),queryCondition.getTestStartTime());
            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0111
                return getSLANWellNumByTimeAndCodeAndItem(queryCondition.getRegentCode(),queryCondition.getTestStartTime(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1000
                return getSLANWellNumByID(queryCondition.getIdcardNum());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1001
                return getSLANWellNumByIDAndItem(queryCondition.getIdcardNum(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1010
                return getSLANWellNumByIDAndTime(queryCondition.getIdcardNum(),queryCondition.getTestStartTime());

            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1011
                return getSLANWellNumByIDAndTimeAndItem(queryCondition.getIdcardNum(),queryCondition.getTestStartTime(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1100
                return getSLANWellNumByIDAndCode(queryCondition.getIdcardNum(),queryCondition.getRegentCode());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1101
                return getSLANWellNumByIDAndCodeAndItem(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),queryCondition.getTestItem());
            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1110
                return getSLANWellNumByIDAndCodeAndTime(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),queryCondition.getTestStartTime());
            }else{
                return getSLANWellNumByAll(queryCondition);
            }
        }
        return 0;
    }

    public List<SLANWellResult> searchSLANWellResultByLength(QueryCondition queryCondition){
        if(queryCondition != null){
            if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0000
                return getAllSLANWellResult(queryCondition.getStart(),queryCondition.getLength());
            }else if (StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0011测试身份证和试剂卡
                return getSLANExperimentWell(queryCondition.getTestItem(), queryCondition.getTestStartTime()
                        ,queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0001
                return getSLANWellResultByItem(queryCondition.getTestItem(),
                        queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0010
                return getSLANWellResultFromTime(queryCondition.getTestStartTime(),
                        queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0100
                return getSLANWellResultFromRegentCode(queryCondition.getRegentCode(),
                        queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0101
                return getSLANByRegentCodeAndTestItem(queryCondition.getRegentCode(),queryCondition.getTestItem()
                        , queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //0110
                return getSLANByTimeAndRegentCode(queryCondition.getRegentCode(),queryCondition.getTestStartTime(),
                        queryCondition.getStart(),queryCondition.getLength());

            }else if(StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //0111
                return getSLANByTimeAndRegentCodeAndItem(queryCondition.getRegentCode(),
                        queryCondition.getTestStartTime(),
                        queryCondition.getTestItem(),
                        queryCondition.getStart(),queryCondition.getLength());

            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1000
                return getSLANByID(queryCondition.getIdcardNum(),
                        queryCondition.getStart(),queryCondition.getLength());

            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1001
                return getSLANByIDAndItem(queryCondition.getIdcardNum(),queryCondition.getTestItem(),
                        queryCondition.getStart(),queryCondition.getLength());

            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1010
                return getSLANByIDAndTime(queryCondition.getIdcardNum(),queryCondition.getTestStartTime(),
                        queryCondition.getStart(),queryCondition.getLength());

            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1011
                return getSLANByIDAndTimeAndItem(queryCondition.getIdcardNum(),
                        queryCondition.getTestStartTime(),queryCondition.getTestItem(),
                        queryCondition.getStart(),queryCondition.getLength());

            } else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1100
                return getSLANByIDAndRegentCode(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),
                        queryCondition.getStart(),queryCondition.getLength());

            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && !StringUtil.isEmpty(queryCondition.getTestItem())){
                //1101
                return getSLANByIDAndRegentCodeAndItem(queryCondition.getIdcardNum(),queryCondition.getRegentCode(),
                        queryCondition.getTestItem(), queryCondition.getStart(),queryCondition.getLength());

            }else if(!StringUtil.isEmpty(queryCondition.getIdcardNum())
                    && !StringUtil.isEmpty(queryCondition.getRegentCode())
                    && !StringUtil.isEmpty(queryCondition.getTestStartTime())
                    && StringUtil.isEmpty(queryCondition.getTestItem())){
                //1110
                return getSLANByIDAndRegentCodeAndTime(queryCondition.getIdcardNum(),
                        queryCondition.getRegentCode(),queryCondition.getTestStartTime(),
                        queryCondition.getStart(),queryCondition.getLength());
            }else{
                return getSLANByAll(queryCondition,queryCondition.getStart(),queryCondition.getLength());
            }


        }
        return  null;

    }

}
