package com.example.trconnector.ui.login;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.example.trconnector.NetManager;
import com.example.trconnector.R;
import com.example.trconnector.databinding.ActivityLoginBinding;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private ArrayAdapter<Server> adapter;
    private List<Server> serverList = new ArrayList<>();

    private static class Server {
        private final int DEFAULT_PORT = 7777;
        public String addr;
        public String name;

        @JSONField(serialize = false)
        private NetManager mgr;

        @Override
        public String toString() {
            return name + "(" + addr + ")";
        }

        public void create() {
            int split = addr.indexOf(':');
            String hostname = split < 0 ? addr : addr.substring(0, split);
            int port = split < 0 ? DEFAULT_PORT : Integer.parseInt(addr.substring(split + 1));
            NetManager mgr = new NetManager();
            mgr.start(hostname, port, name);
            this.mgr = mgr;
        }
    }

    private static Boolean isValid(String addr, String name){
        if (addr.isEmpty() || name.isEmpty()) return false;
        try {
            int split = addr.indexOf(':');
            if (split > 0) {
                int port = Integer.parseInt(addr.substring(split + 1));
                if (port < 0 || port > 65535) return false;
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    private void save() {
        SharedPreferences.Editor editor = this.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString("content", JSON.toJSONString(this.serverList));
        editor.commit();
    }

    private void load() {
        String content = this.getPreferences(Context.MODE_PRIVATE).getString("content", null);
        if (content == null) return;
        List<Server> serverList = JSON.parseArray(content, Server.class);
        this.serverList = new ArrayList<>();
        for (Server server : serverList) {
            try {
                server.create();
            } catch (Exception e) {
                new AlertDialog.Builder(this)
                        .setTitle("错误").setMessage(e.toString()).show();
                continue;
            }
            this.serverList.add(server);
        }

    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final EditText address = binding.address;
        final EditText serverName = binding.name;
        final Button add = binding.add;
        final ListView serverList = binding.serverList;

        this.load();

        final TextWatcher watcher =new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                add.setEnabled(isValid(address.getText().toString(), serverName.getText().toString()));
            }
        };

        address.addTextChangedListener(watcher);
        serverName.addTextChangedListener(watcher);

        adapter = new ArrayAdapter<Server>(this, android.R.layout.simple_list_item_1, this.serverList);
        serverList.setAdapter(adapter);

        final LoginActivity context = this;
        serverList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == -1) return false;
                new AlertDialog.Builder(context)
                        .setTitle("确认")
                        .setMessage("是否要删除" + context.serverList.get(i).toString() + "?")
                        .setPositiveButton("是", (view1, var2) -> {
                            context.serverList.get(i).mgr.stop();
                            context.serverList.remove(i);
                            context.save();
                            context.adapter.notifyDataSetChanged();
                        })
                        .setNegativeButton("否", null)
                        .show();
                return true;
            }
        });
        add.setOnClickListener(v -> {
            String addr = address.getText().toString();
            String name = serverName.getText().toString();
            Server t = new Server();
            t.addr = addr;
            t.name = name;

            try {
                t.create();
            } catch (Exception e) {
                new AlertDialog.Builder(context)
                        .setTitle("错误").setMessage(e.toString()).show();
                return;
            }

            this.serverList.add(t);
            this.save();
            this.adapter.notifyDataSetChanged();

            address.getText().clear();
            serverName.getText().clear();
            add.setEnabled(false);
        });

        add.setEnabled(false);
    }
}