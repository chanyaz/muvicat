package xyz.arnau.muvicat.cache.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

import xyz.arnau.muvicat.cache.dao.CinemaDao;
import xyz.arnau.muvicat.cache.dao.MovieDao;
import xyz.arnau.muvicat.cache.dao.PostalCodeDao;
import xyz.arnau.muvicat.cache.dao.ShowingDao;
import xyz.arnau.muvicat.cache.db.migrations.DbMigration1to2;
import xyz.arnau.muvicat.cache.db.migrations.DbMigration2to3;
import xyz.arnau.muvicat.cache.db.migrations.DbMigration3to4;
import xyz.arnau.muvicat.cache.db.migrations.DbMigration4to5;
import xyz.arnau.muvicat.cache.db.migrations.DbMigration5to6;
import xyz.arnau.muvicat.cache.model.CastMemberEntity;
import xyz.arnau.muvicat.cache.model.CinemaEntity;
import xyz.arnau.muvicat.cache.model.MovieEntity;
import xyz.arnau.muvicat.cache.model.PostalCodeEntity;
import xyz.arnau.muvicat.cache.model.ShowingEntity;
import xyz.arnau.muvicat.cache.utils.PostalCodeCsvReader;
import xyz.arnau.muvicat.utils.AppExecutors;

@Database(
        entities = {MovieEntity.class, CinemaEntity.class, PostalCodeEntity.class,
                ShowingEntity.class, CastMemberEntity.class},
        version = 6
)
@TypeConverters({DateTypeConverter.class, StringListTypeConverter.class})
public abstract class MuvicatDatabase extends RoomDatabase {
    private static final String MUVICAT_DB = "muvicat-db";
    private static MuvicatDatabase sInstance = null;

    public abstract MovieDao movieDao();

    public abstract CinemaDao cinemaDao();

    public abstract PostalCodeDao postalCodeDao();

    public abstract ShowingDao showingDao();

    @SuppressWarnings("SynchronizedMethod")
    public static synchronized MuvicatDatabase getInstance(Context context, AppExecutors appExecutors) {
        if (sInstance == null) {
            sInstance = buildDatabase(context, appExecutors);
        }
        return sInstance;
    }

    private static MuvicatDatabase buildDatabase(Context context, AppExecutors appExecutors) {
        return Room.databaseBuilder(context, MuvicatDatabase.class, MUVICAT_DB)
                .addMigrations(DbMigration1to2.INSTANCE, DbMigration2to3.INSTANCE, DbMigration3to4.INSTANCE,
                        DbMigration4to5.INSTANCE, DbMigration5to6.INSTANCE)
                .addCallback(new PostalCodesDbCallback(context, appExecutors, new PostalCodeCsvReader()))
                .build();
    }
}

