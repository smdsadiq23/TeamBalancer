package com.example.teambalancer;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Club.class, Player.class, User.class}, version = 4, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final Migration MIGRATION_3_4 =
            new Migration(3, 4) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase db) {
                    db.execSQL("ALTER TABLE clubs ADD COLUMN remoteId INTEGER");
                    db.execSQL("ALTER TABLE players ADD COLUMN remoteId INTEGER");
                }
            };

    private static AppDatabase instance;

    /** Single writer avoids transaction races and matches Room’s recommended usage. */
    public static final ExecutorService databaseWriteExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "TeamBalancer-DB");
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            });

    public abstract ClubDao clubDao();

    public abstract PlayerDao playerDao();

    public abstract UserDao userDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance =
                    Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "team_balancer_db")
                            .addMigrations(MIGRATION_3_4)
                            .fallbackToDestructiveMigration()
                            .build();
        }
        return instance;
    }
}
