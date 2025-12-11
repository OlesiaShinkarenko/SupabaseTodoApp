package com.example.supabasetodoapp;

import static android.widget.Toast.LENGTH_SHORT;
import static com.example.supabasetodoapp.network.SupabaseConfig.TASKS_URL;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supabasetodoapp.adapters.TasksAdapter;
import com.example.supabasetodoapp.models.Task;
import com.example.supabasetodoapp.network.SupabaseConfig;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TasksAdapter adapter;
    private String accessToken;
    private String userId;
    private ArrayList<Task> tasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        accessToken = prefs.getString("access_token", null);
        userId = prefs.getString("user_id", null);
        if (accessToken == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        FloatingActionButton fab = findViewById(R.id.fabAddItem);
        fab.setOnClickListener(v -> showAddDialog());

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TasksAdapter(tasks);
        recyclerView.setAdapter(adapter);
        loadTasks();
    }

    private void showAddDialog() {
        final EditText etTitle = new EditText(this);
        etTitle.setHint("Название задачи");

        final EditText etPriority = new EditText(this);
        etPriority.setHint("Приоритет (число)");

        final EditText etDueDate = new EditText(this);
        etDueDate.setHint("Дата (YYYY-MM-DD)");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);
        container.addView(etTitle);
        container.addView(etPriority);
        container.addView(etDueDate);

        new AlertDialog.Builder(this)
                .setTitle("Новая задача")
                .setView(container)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String prioStr = etPriority.getText().toString().trim();
                    String dueDate = etDueDate.getText().toString().trim();

                    if (title.isEmpty() || prioStr.isEmpty()) {
                        Toast.makeText(this, "Заполните название и приоритет", LENGTH_SHORT).show();
                    }

                    int priority = Integer.parseInt(prioStr);
                    addTask(title, priority, dueDate);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    public void showEditDialog(String id, String currentTitle, int currentPriority, String currentDueDate) {
        final EditText etTitle = new EditText(this);
        etTitle.setHint("Название задачи");
        etTitle.setText(currentTitle);

        final EditText etPriority = new EditText(this);
        etPriority.setHint("Приоритет");
        etPriority.setInputType(InputType.TYPE_CLASS_NUMBER);
        etPriority.setText(String.valueOf(currentPriority));

        final EditText etDueDate = new EditText(this);
        etDueDate.setText("Дата (YYYY-MM-DD)");
        etDueDate.setText(currentDueDate);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);
        container.addView(etTitle);
        container.addView(etPriority);
        container.addView(etDueDate);

        new AlertDialog.Builder(this)
                .setTitle("Редактирование задачи")
                .setView(container)
                .setPositiveButton("Обновить", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String prioStr = etPriority.getText().toString().trim();
                    String dueDate = etDueDate.getText().toString().trim();

                    if (title.isEmpty() || prioStr.isEmpty()) {
                        Toast.makeText(this, "Заполните название и приоритет",
                                LENGTH_SHORT).show();
                    }

                    int priority = Integer.parseInt(prioStr);
                    updateTask(id, title, priority, dueDate);
                }).setNegativeButton("Отмена", null)
                .show();
    }

    private void updateTask(String id, String title, int priority, String dueDate) {
        networkExecutor.execute(() -> {
                    HttpURLConnection conn = null;
                    try {
                        URL url = new URL(TASKS_URL + "&id=eq." + id);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("PATCH");
                        conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);

                        JSONObject json = new JSONObject();
                        json.put("title", title);
                        json.put("priority", priority);
                        json.put("due_date", dueDate);

                        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
                        conn.getOutputStream().write(body);

                        int code = conn.getResponseCode();

                        InputStream err = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
                        String errorBody = err != null ? new BufferedReader(new InputStreamReader(err))
                                .lines().collect(Collectors.joining("\n")) : "";
                        Log.e("UPDATE_ERROR", "code=" + code + " body=" + errorBody);

                        if (code == 200 || code == 204) {
                            mainHandler.post(this::loadTasks);
                        } else {
                            mainHandler.post(() ->
                                    Toast.makeText(
                                            this,
                                            "Ошибка обновления: " + code,
                                            LENGTH_SHORT
                                    ).show()
                            );
                        }
                    } catch (Exception e) {
                        mainHandler.post(() ->
                                Toast.makeText(this,
                                        "Ошибка обновления: " + e.getMessage(),
                                        LENGTH_SHORT).show()

                        );
                    } finally {
                        if (conn != null) conn.disconnect();
                    }
                }
        );
    }

    private void addTask(String title, int priority, String dueDate) {
        networkExecutor.execute(() -> {
                    HttpURLConnection conn = null;
                    try {
                        URL url = new URL(TASKS_URL);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);

                        JSONObject json = new JSONObject();
                        json.put("title", title);
                        json.put("priority", priority);
                        json.put("due_date", dueDate);
                        json.put("user_id", userId);

                        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
                        conn.getOutputStream().write(body);

                        int code = conn.getResponseCode();
                        if (code == 201 || code == 200) {
                            mainHandler.post(this::loadTasks);
                        } else {
                            mainHandler.post(() ->
                                    Toast.makeText(this, "Ошибка добавления: " + code, LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        mainHandler.post(() ->
                                Toast.makeText(this, e.getMessage(),
                                        LENGTH_SHORT
                                ).show()
                        );
                    } finally {
                        if (conn != null) conn.disconnect();
                    }
                }
        );
    }

    private void loadTasks() {
        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(TASKS_URL + "?select=*");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);

                int code = connection.getResponseCode();
                InputStream is = (code >= 200 && code < 300)
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                String response = readStream(is);

                if (code == 200) {
                    ArrayList<Task> loaded = parseTasks(response);
                    mainHandler.post(() -> {
                        tasks.clear();
                        tasks.addAll(loaded);
                        adapter.notifyDataSetChanged();
                    });
                } else {
                    int finalCode = code;
                    mainHandler.post(() ->
                            Toast.makeText(this, "Ошибка загрузки: " + finalCode, LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), LENGTH_SHORT).show()
                );
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    public void deleteTask(String id) {
        networkExecutor.execute(() -> {
                    HttpURLConnection conn = null;
                    try {
                        URL url = new URL(TASKS_URL + "&id=eq." + id);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("DELETE");
                        conn.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

                        int code = conn.getResponseCode();
                        if (code == 200 || code == 204) {
                            mainHandler.post(this::loadTasks);
                        } else {
                            mainHandler.post(() ->
                                    Toast.makeText(this, "Ошибка удаления: " + code, Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        mainHandler.post(() ->
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
                    } finally {
                        {
                            if (conn != null) conn.disconnect();
                        }
                    }
                }
        );
    }

    private ArrayList<Task> parseTasks(String json) throws Exception {
        ArrayList<Task> result = new ArrayList<>();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            String id = obj.getString("id");
            String title = obj.getString("title");
            int priority = obj.getInt("priority");
            String dueDate = obj.getString("due_date");
            result.add(new Task(id, title, priority, dueDate));
        }
        return result;
    }

    private String readStream(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

}