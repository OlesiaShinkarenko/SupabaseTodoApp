package com.example.supabasetodoapp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.supabasetodoapp.MainActivity;
import com.example.supabasetodoapp.R;
import com.example.supabasetodoapp.models.Task;

import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    private List<Task> tasks;

    public TasksAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvPriority.setText("Priority: " + task.getPriority());
        holder.tvDueDate.setText("Due: " + task.getDueDate());
        holder.itemId = task.getId();


        holder.itemView.setOnLongClickListener(v -> {
                    ((MainActivity) holder.itemView.getContext())
                            .showEditDialog(task.getId(), task.getTitle(), task.getPriority(), task.getDueDate());
                    return true;
                }
        );

        holder.btnDelete.setOnClickListener(v -> {
            Context ctx = v.getContext();
            new AlertDialog.Builder(ctx)
                    .setTitle("Удалить задачу?")
                    .setMessage(task.getTitle())
                    .setPositiveButton(
                            "Да", (d, i) ->
                            {
                                if (ctx instanceof MainActivity) {
                                    ((MainActivity) ctx).deleteTask(task.getId());
                                }
                            }
                    ).setNegativeButton("Нет", null).show();
        });
    }

    @Override
    public int getItemCount() {
        return tasks == null ? 0 : tasks.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {

        String itemId;
        TextView tvTitle;
        TextView tvPriority;
        TextView tvDueDate;
        ImageButton btnDelete;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
