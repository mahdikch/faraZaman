package net.osmtracker.data.db.room

//import androidx.room.Database
//import androidx.room.RoomDatabase
import net.osmtracker.data.db.dao.UserDao
import net.osmtracker.data.db.model.UserEntity

//@Database(entities = [UserEntity::class], version = 1)
abstract class AppDatabase
//    : RoomDatabase()
{
    abstract fun userDao(): UserDao
}