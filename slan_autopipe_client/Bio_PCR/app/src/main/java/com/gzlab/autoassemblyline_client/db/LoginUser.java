package com.gzlab.autoassemblyline_client.db;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.NotNull;
import com.litesuits.orm.db.annotation.Table;

/**
 * 登录账号，可以进行普通用户账号的编辑删除和添加
 */
@Table("LoginUser")//反应孔标识
public class LoginUser extends BaseModel{
    @NotNull
    @Column("userAccount")
    private String userAccount;//普通用户账号
    @NotNull
    @Column("userPassword")
    private String userPassword;//普通用户密码


    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }
}
