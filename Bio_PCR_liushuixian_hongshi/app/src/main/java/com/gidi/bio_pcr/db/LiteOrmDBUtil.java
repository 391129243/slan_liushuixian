package com.gidi.bio_pcr.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.constant.Constants;
import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.DataBaseConfig;
import com.litesuits.orm.db.assit.QueryBuilder;
import com.litesuits.orm.db.assit.WhereBuilder;
import com.litesuits.orm.db.model.ColumnsValue;
import com.litesuits.orm.db.model.ConflictAlgorithm;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class LiteOrmDBUtil {
    private static String DB_NAME;
    private static LiteOrm liteOrm;
    private static Context mContext;
    private static String DB_NAME_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"+ Constants.APP_NAME+ "/database";

    /**
     * 数据库名称
     *
     * @return
     */
    private static String getUserDatabaseName() {
        return "autopipeline.db";
    }

    /**
     * 创建级联数据库
     *
     * @param context
     */
    public static boolean createCascadeDB(Context context) {
        mContext = context.getApplicationContext();
        DB_NAME = getUserDatabaseName();
        liteOrm = LiteOrm.newCascadeInstance(mContext, DB_NAME);
       // liteOrm.setDebugged(LogUtil.isDebuggable());
        return true;
    }

     /* 创建数据库细节 ：
            * 细节一：每次启动都会调用这个方法，是否会重复创建，或者说怎么判断这个数据库已经存在。
            * 回答：尽在第一次创建，开发这不用关心存在否。
            *
            * 细节二：如果指定目录创建在SD卡上，卸载APP之后，重新安装，以前的创建的数据库还存在
	 * 回答：如果不想这样，就不要指定创建在SD卡上面，默认创建就在APP里。卸载后数据库就不存在了
	 *
             * 细节三：创建数据库有两种模式，第一种是单一操作没有级联关系的，第二种是级联操作。
            * 创建单一操作模式：LiteOrm.newSingleInstance(_activity, DB_NAME);
	 * 创建级联操作模式：LiteOrm.newCascadeInstance(_activity, DB_NAME);
	 */
    public static LiteOrm getLiteOrm() {
        if (liteOrm == null) {
            if (mContext == null) {
                mContext = PcrApplication.getAppContext();
            }
            DataBaseConfig config = new DataBaseConfig(mContext);
            //数据库名，可设置存储路径。默认在内部存储位置databases文件夹下
            DB_NAME = getUserDatabaseName();
            config.dbName = DB_NAME_PATH + File.separator + DB_NAME;
            config.debugged = true; //是否打Log
            config.dbVersion = 1; // database Version
            config.onUpdateListener = null; //升级
            liteOrm = LiteOrm.newSingleInstance(config);
            liteOrm.setDebugged(true);
           // liteOrm = LiteOrm.newSingleInstance(mContext, DB_NAME);
         // liteOrm.setDebugged(LogUtil.isDebuggable());
        }
        return liteOrm;
    }


    /**
     * 插入一条记录
     *
     * @param t
     */
    public static <T> long insert(T t) {
        return getLiteOrm().save(t);
    }

    /**
     * 插入所有记录
     *
     * @param list
     */
    public static <T> int insertAll(List<T> list) {
        return getLiteOrm().save(list);
    }

    /**
     * 以某种条件作为插入标准
     *
     * @param
     * @return
     */
    public static <T> long insertAll(Collection<T> t, ConflictAlgorithm config) {
        return getLiteOrm().insert(t, config);
    }

    /**
     * 以某种条件作为插入标准
     *
     * @param
     * @return
     */
    public static <T> long insertAll(List<T> t, ConflictAlgorithm config) {
        return getLiteOrm().insert(t, config);
    }


    public static <T> List<T> getQuery(QueryBuilder<T> queryBuilder){
        return getLiteOrm().query(queryBuilder);
    }
    /**
     * 查询所有
     *
     * @param cla
     * @return
     */
    public static <T> List<T> getQueryAll(Class<T> cla) {
        return getLiteOrm().query(cla);
    }

    /**
     * 根据ID查询
     *
     * @param cla
     * @return
     */
    public static <T> T getInfoById(String id, Class<T> cla) {
        return getLiteOrm().queryById(id, cla);
    }

    /**
     * 根据ID查询
     *
     * @param cla
     * @return
     */
    public static <T> T getInfoById(long id, Class<T> cla) {
        return getLiteOrm().queryById(id, cla);
    }

    /**
     * 查询 某字段 等于 Value的值
     *
     * @param cla
     * @param field
     * @param value
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> List<T> getQueryByWhere(Class<T> cla, String field, Object[] value) {
        return getLiteOrm().<T>query(new QueryBuilder(cla).where(field + "=?", value));
    }

    /**
     * 模糊查询
     *
     * @param cla
     * @param field
     * @param value
     * @param <T>
     * @return
     */
    public static <T> List<T> getQueryByTime(Class<T> cla, String field, Object[] value) {
        return getLiteOrm().<T>query(new QueryBuilder(cla).where(field + " LIKE ?", value));
    }




    /**
     * 查询 某字段 等于 Value的值 可以指定从1-20，就是分页
     *
     * @param cla
     * @param field
     * @param value
     * @param start
     * @param length
     * @return
     */
    public static <T> List<T> getQueryByWhereLength(Class<T> cla, String field, Object[] value, int start, int length) {
        return getLiteOrm().<T>query(new QueryBuilder(cla).where(field + "=?", value).limit(start, length));
    }

    /**
     * 删除所有 某字段等于 Vlaue的值
     *
     * @param cla
     * @param field
     * @param value
     */
    @SuppressWarnings("deprecation")
    public static <T> int deleteWhere(Class<T> cla, String field, String[] value) {
        return getLiteOrm().delete(cla, WhereBuilder.create(cla, field + "=?", value));
    }


    public static <T> int delete(WhereBuilder whereBuilder){
        return getLiteOrm().delete(whereBuilder);
    }

    /**
     * 删除所有
     *
     * @param cla
     */
    public static <T> int deleteAll(Class<T> cla) {
        return getLiteOrm().deleteAll(cla);
    }

    /**
     * 仅在以存在时更新
     *
     * @param t
     */
    public static <T> int update(T t) {
        return getLiteOrm().update(t, ConflictAlgorithm.Replace);
    }

    /**
     * 以某种条件来整体更新
     *
     * @param list
     * @param config
     * @return
     */
    public static <T> int updateAll(List<T> list, ConflictAlgorithm config) {
        return getLiteOrm().update(list, config);
    }

    public static <T> int updateALL(List<T> list) {
        return getLiteOrm().update(list);
    }

    public static <T> void update(Class<T> cla, String queryCol,
                                  String queryValue, String updateCol, String updateValue) {
        getLiteOrm().update(
                new WhereBuilder(cla).where(queryCol + " = ?",
                        new Object[]{queryValue}),
                new ColumnsValue(new String[]{updateCol},
                        new Object[]{updateValue}), ConflictAlgorithm.None);

    }

    private static  <T> void update(Class<T> cla, WhereBuilder whereBuilder){

    }

    @SuppressLint("NewApi")
    public void closeDB() {
        if (liteOrm != null) {
            liteOrm.close();
        }
    }

}
