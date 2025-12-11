package com.example.supabasetodoapp;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.supabasetodoapp.network.SupabaseConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnSignUp;
    private Button btnSignIn;

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignIn = findViewById(R.id.btnSignIn);

        btnSignUp.setOnClickListener(v -> signUp());
        btnSignIn.setOnClickListener(v -> signIn());
    }

    private void signUp() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email и пароль обязательны", Toast.LENGTH_SHORT).show();
            return;
        }

        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(SupabaseConfig.AUTH_SIGNUP_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.toString().getBytes());
                }

                int code = connection.getResponseCode();
                InputStream is = (code >= 200 && code < 300)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                readStream(is);

                int finalCode = code;
                mainHandler.post(() -> {
                    if (finalCode == 200 || finalCode == 201) {
                        Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Ошибка регистрации: " + finalCode, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void signIn() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email и пароль обязательны", Toast.LENGTH_SHORT).show();
            return;
        }

        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(SupabaseConfig.AUTH_SIGNIN_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.toString().getBytes());
                }

                int code = connection.getResponseCode();
                InputStream is = (code >= 200 && code < 300)
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                String response = readStream(is);

                if (code == 200) {
                    JSONObject obj = new JSONObject(response);
                    String accessToken = obj.getString("access_token");
                    String userId = obj.getJSONObject("user").getString("id");

                    SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
                    prefs.edit()
                            .putString("access_token", accessToken)
                            .putString("user_id", userId)
                            .apply();

                    mainHandler.post(() -> {
                        Toast.makeText(this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    });
                } else {
                    int finalCode = code;
                    mainHandler.post(() ->
                            Toast.makeText(this, "Ошибка входа: " + finalCode, Toast.LENGTH_SHORT).show()
                    );
                }

            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private String readStream(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}