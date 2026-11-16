package com.gzlab.autoassemblyline_client.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gzlab.autoassemblyline_client.R;
import com.gzlab.autoassemblyline_client.db.LoginUser;
import com.gzlab.autoassemblyline_client.util.CommonUtil;

import java.util.ArrayList;
import java.util.List;

public class UserManagerAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<LoginUser> loginUsers;
    private Context context;
    private int selectedPosition = -1;

    public UserManagerAdapter(Context context){
        this.context = context;
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        loginUsers = new ArrayList<LoginUser>();
    }

    public void setLoginUserList(List<LoginUser> list) {
        this.loginUsers = list;
        this.notifyDataSetChanged();
    }



    @Override
    public int getCount() {
        return loginUsers.size();
    }

    @Override
    public Object getItem(int i) {
        return loginUsers.get(i);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if(null != view){
            holder = (ViewHolder) view.getTag();
        }else{
            holder = new ViewHolder();
            view = mInflater.inflate(R.layout.item_list_account_manager,viewGroup,false);

            holder.indexTv = (TextView)view.findViewById(R.id.item_account_manager_index_tv);
            holder.accountTv =  (TextView)view.findViewById(R.id.item_account_manager_name_tv);
            holder.passwordTv = (TextView)view.findViewById(R.id.item_account_manager_password_tv);

            holder.deleteTv =  (TextView)view.findViewById(R.id.item_account_manager_delete_tv);
            holder.editTv = (TextView)view.findViewById(R.id.item_account_manager_edit_tv);
            view.setTag(holder);
        }
        LoginUser user = loginUsers.get(position);
        holder.indexTv.setText(position+ 1 + "");
        holder.accountTv.setText(user.getUserAccount());
        try {
            holder.passwordTv.setText(CommonUtil.decryptAEC(user.getUserPassword()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        holder.deleteTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
                if (null != onItemClickListener){
                    onItemClickListener.onItemDelete(position);
                }
            }
        });

        holder.editTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
                if (null != onItemClickListener){
                    try {
                        onItemClickListener.onItemEdit(position);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        return view;
    }

    class ViewHolder{
        TextView indexTv;//序号
        TextView accountTv;//账号
        TextView passwordTv;//密码
        TextView deleteTv;//删除
        TextView editTv;//编辑
    }

    private OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }
    public interface OnItemClickListener {

        void onItemDelete(int item_position);

        void onItemEdit(int item_position) throws Exception;
    }
}
