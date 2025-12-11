package com.example.supabasetodoapp.network;

public class SupabaseConfig {
        public static final String SUPABASE_URL = "https://gwasunnmrsvijvqgoqsn.supabase.co";
        public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd3YXN1bm5tcnN2aWp2cWdvcXNuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU0MTI3OTAsImV4cCI6MjA4MDk4ODc5MH0.jhd1pkszKP4wbriLqWW3sdC3ajAsbuKA_QsGcO4hEEE";

        public static final String AUTH_SIGNUP_URL = SUPABASE_URL + "/auth/v1/signup";
        public static final String AUTH_SIGNIN_URL = SUPABASE_URL + "/auth/v1/token?grant_type=password";
        public static final String TASKS_URL = SUPABASE_URL + "/rest/v1/tasks?select=*";
}
